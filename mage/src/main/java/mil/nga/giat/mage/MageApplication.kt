package mil.nga.giat.mage

import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import mil.nga.giat.mage.data.datasource.observation.AttachmentLocalDataSource
import mil.nga.giat.mage.data.repository.layer.LayerRepository
import mil.nga.giat.mage.data.repository.user.UserRepository
import mil.nga.giat.mage.di.TokenProvider
import mil.nga.giat.mage.feed.FeedFetchService
import mil.nga.giat.mage.location.LocationFetchService
import mil.nga.giat.mage.location.LocationReportingService
import mil.nga.giat.mage.login.AccountStateActivity
import mil.nga.giat.mage.login.LoginActivity
import mil.nga.giat.mage.login.SignupActivity
import mil.nga.giat.mage.login.idp.IdpLoginActivity
import mil.nga.giat.mage.network.Server
import mil.nga.giat.mage.observation.ObservationNotificationListener
import mil.nga.giat.mage.observation.sync.AttachmentSyncListener
import mil.nga.giat.mage.observation.sync.AttachmentSyncWorker
import mil.nga.giat.mage.observation.sync.ObservationFetchService
import mil.nga.giat.mage.observation.sync.ObservationFetchWorker
import mil.nga.giat.mage.observation.sync.ObservationSyncListener
import mil.nga.giat.mage.observation.sync.ObservationSyncWorker
import mil.nga.giat.mage.data.datasource.observation.ObservationLocalDataSource
import mil.nga.giat.mage.data.datasource.user.UserLocalDataSource
import mil.nga.giat.mage.di.TokenStatus
import mil.nga.giat.mage.login.ServerUrlActivity
import javax.inject.Inject

@HiltAndroidApp
class MageApplication : Application(),
   Configuration.Provider,
   SharedPreferences.OnSharedPreferenceChangeListener,
   LifecycleEventObserver,
   Application.ActivityLifecycleCallbacks
{
   private var observationNotificationListener: ObservationNotificationListener? = null
   private var runningActivity: Activity? = null

   private var tokenObserver = Observer<TokenStatus> { tokenStatus ->
      if (tokenStatus !is TokenStatus.Active) {
         Log.d(LOG_NAME, "Application token expired, remove session")
         invalidateSession(
            activity = runningActivity,
            applicationInUse = tokenStatus is TokenStatus.Expired
         )
      }
   }

   @Inject lateinit var server: Server
   @Inject lateinit var tokenProvider: TokenProvider
   @Inject lateinit var preferences: SharedPreferences
   @Inject lateinit var userRepository: UserRepository
   @Inject lateinit var layerRepository: LayerRepository
   @Inject lateinit var userLocalDataSource: UserLocalDataSource
   @Inject lateinit var observationLocalDataSource: ObservationLocalDataSource
   @Inject lateinit var attachmentLocalDataSource: AttachmentLocalDataSource

   @EntryPoint
   @InstallIn(SingletonComponent::class)
   interface HiltWorkerFactoryEntryPoint {
      fun workerFactory(): HiltWorkerFactory
   }

   override val workManagerConfiguration = Configuration.Builder()
      .setWorkerFactory(EntryPoints.get(this, HiltWorkerFactoryEntryPoint::class.java).workerFactory())
      .build()

   override fun onCreate() {
      super.onCreate()

      ObservationSyncListener(observationLocalDataSource) {
         ObservationSyncWorker.scheduleWork(applicationContext)
      }
      AttachmentSyncListener(attachmentLocalDataSource) {
         AttachmentSyncWorker.scheduleWork(applicationContext)
      }
      ProcessLifecycleOwner.get().lifecycle.addObserver(this)

      // setup the screen unlock stuff
      registerActivityLifecycleCallbacks(this)
      val dayNightTheme: Int = preferences.getInt(
         resources.getString(R.string.dayNightThemeKey),
         resources.getInteger(R.integer.dayNightThemeDefaultValue)
      )
      AppCompatDelegate.setDefaultNightMode(dayNightTheme)
      val notificationManager: NotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
      val channel = NotificationChannel(
         MAGE_NOTIFICATION_CHANNEL_ID,
         "MAGE",
         NotificationManager.IMPORTANCE_LOW
      )
      channel.setShowBadge(true)
      notificationManager.createNotificationChannel(channel)
      val observationChannel = NotificationChannel(
         MAGE_OBSERVATION_NOTIFICATION_CHANNEL_ID,
         "MAGE Observations",
         NotificationManager.IMPORTANCE_HIGH
      )
      observationChannel.setShowBadge(true)
      notificationManager.createNotificationChannel(observationChannel)
   }

   fun onLogin() {
      //set up observation notifications
      if (observationNotificationListener == null) {
         val listener =  ObservationNotificationListener(applicationContext)
         observationLocalDataSource.addListener(listener)
         observationNotificationListener = listener
      }

      // Start fetching observations and locations
      startFetching()
      ObservationFetchWorker.beginWork(applicationContext)
   }


   fun onLogout(
      clearTokenInformationAndSendLogoutRequest: Boolean
   ) {
      observationNotificationListener?.let { observationLocalDataSource.removeListener(it) }
      observationNotificationListener = null

      destroyFetching()
      destroyNotification()
      stopLocationService()
      ObservationFetchWorker.stopWork(applicationContext)

      if (clearTokenInformationAndSendLogoutRequest) {
         userRepository.signout()
      }

      preferences.edit()
         .putBoolean(getString(R.string.disclaimerAcceptedKey), false)
         .apply()

      userLocalDataSource.removeCurrentEvent()
   }

   private fun destroyNotification() {
      val notificationManager: NotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
      notificationManager.cancel(MAGE_SUMMARY_NOTIFICATION_ID)
      notificationManager.cancel(MAGE_ACCOUNT_NOTIFICATION_ID)
      notificationManager.cancel(ObservationNotificationListener.OBSERVATION_NOTIFICATION_ID)
   }

   private fun startFetching() {
      startService(Intent(applicationContext, LocationFetchService::class.java))
      startService(Intent(applicationContext, ObservationFetchService::class.java))
      startService(Intent(applicationContext, FeedFetchService::class.java))
   }

   /**
    * Stop Tasks responsible for fetching Observations and Locations from the server.
    */
   private fun destroyFetching() {
      stopService(Intent(applicationContext, LocationFetchService::class.java))
      stopService(Intent(applicationContext, ObservationFetchService::class.java))
      stopService(Intent(applicationContext, FeedFetchService::class.java))
   }

   fun startLocationService() {
      val intent = Intent(applicationContext, LocationReportingService::class.java)
      ContextCompat.startForegroundService(applicationContext, intent)
   }

   fun stopLocationService() {
      val intent = Intent(applicationContext, LocationReportingService::class.java)
      stopService(intent)
   }

   override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
      if (getString(R.string.reportLocationKey).equals(key, ignoreCase = true) && !tokenProvider.isExpired()) {
         val reportLocation = sharedPreferences?.getBoolean(
            getString(R.string.reportLocationKey),
            resources.getBoolean(R.bool.reportLocationDefaultValue)
         )
         if (reportLocation == true) {
            startLocationService()
         } else {
            stopLocationService()
         }
      }
   }

   override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
   override fun onActivityStarted(activity: Activity) {}

   override fun onActivityResumed(activity: Activity) {
      if (tokenProvider.isExpired()) {
         invalidateSession(activity, false)
      }
      runningActivity = activity
   }

   override fun onActivityPaused(activity: Activity) {
      runningActivity = null
   }

   override fun onActivityStopped(activity: Activity) {}
   override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
   override fun onActivityDestroyed(activity: Activity) {}

   private fun invalidateSession(activity: Activity?, applicationInUse: Boolean) {
      destroyFetching()
      ObservationFetchWorker.stopWork(applicationContext)

      // TODO JWT where else is disclaimer accepted set to false.
      // Why not set to false if activity resumed onActivityResumed and token is invalid?
      preferences.edit().putBoolean(getString(R.string.disclaimerAcceptedKey), false).apply()

      if (activity !is LoginActivity &&
          activity !is IdpLoginActivity &&
          activity !is AccountStateActivity &&
          activity !is SignupActivity &&
          activity !is ServerUrlActivity
      ) {
         forceLogin(applicationInUse)
      }
   }

   private fun forceLogin(applicationInUse: Boolean) {
      val intent = Intent(this, LoginActivity::class.java)
      intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
      intent.putExtra(LoginActivity.EXTRA_CONTINUE_SESSION, true)
      intent.putExtra(LoginActivity.EXTRA_CONTINUE_SESSION_WHILE_USING, applicationInUse)
      startActivity(intent)
   }

   companion object {
      private val LOG_NAME = MageApplication::class.java.name

      const val MAGE_SUMMARY_NOTIFICATION_ID = 100
      const val MAGE_ACCOUNT_NOTIFICATION_ID = 101
      const val MAGE_OBSERVATION_NOTIFICATION_PREFIX = 10000
      const val MAGE_NOTIFICATION_GROUP = "mil.nga.mage.MAGE_NOTIFICATION_GROUP"
      const val MAGE_OBSERVATION_NOTIFICATION_GROUP = "mil.nga.mage.MAGE_OBSERVATION_NOTIFICATION_GROUP"
      const val MAGE_NOTIFICATION_CHANNEL_ID = "mil.nga.mage.MAGE_NOTIFICATION_CHANNEL"
      const val MAGE_OBSERVATION_NOTIFICATION_CHANNEL_ID = "mil.nga.mage.MAGE_OBSERVATION_NOTIFICATION_CHANNEL"
   }

   override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
      when (event) {
         Lifecycle.Event.ON_START -> {
            preferences.registerOnSharedPreferenceChangeListener(this)

            tokenProvider.observe(source, tokenObserver)

            // Start fetching and pushing observations and locations
            if (!tokenProvider.isExpired()) {
               startFetching()
            }
         }
         Lifecycle.Event.ON_DESTROY -> {
            tokenProvider.removeObserver(tokenObserver)
            preferences.registerOnSharedPreferenceChangeListener(this)
            destroyFetching()
         }
         else -> {}
      }
   }
}