package mil.nga.giat.mage.login

import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.work.WorkManager
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mil.nga.giat.mage.LandingActivity
import mil.nga.giat.mage.MageApplication
import mil.nga.giat.mage.R
import mil.nga.giat.mage.R.xml
import mil.nga.giat.mage.cache.CacheUtils
import mil.nga.giat.mage.compat.server5.login.SignupActivityServer5
import mil.nga.giat.mage.contact.ContactDialog
import mil.nga.giat.mage.data.datasource.user.UserLocalDataSource
import mil.nga.giat.mage.database.dao.MageSqliteOpenHelper
import mil.nga.giat.mage.database.model.event.Event
import mil.nga.giat.mage.di.TokenProvider
import mil.nga.giat.mage.disclaimer.DisclaimerActivity
import mil.nga.giat.mage.event.EventsActivity
import mil.nga.giat.mage.login.AuthenticationStatus.AccountCreated
import mil.nga.giat.mage.login.AuthenticationStatus.Offline
import mil.nga.giat.mage.login.AuthorizationStatus.FailAuthentication
import mil.nga.giat.mage.login.AuthorizationStatus.FailAuthorization
import mil.nga.giat.mage.login.AuthorizationStatus.FailInvalidServer
import mil.nga.giat.mage.login.LoginViewModel.Authentication
import mil.nga.giat.mage.login.LoginViewModel.AuthenticationState
import mil.nga.giat.mage.login.LoginViewModel.Authorization
import mil.nga.giat.mage.login.idp.IdpLoginFragment
import mil.nga.giat.mage.login.ldap.LdapLoginFragment
import mil.nga.giat.mage.login.mage.MageLoginFragment
import mil.nga.giat.mage.map.cache.CacheProvider
import mil.nga.giat.mage.sdk.Compatibility.Companion.isServerVersion5
import mil.nga.giat.mage.sdk.preferences.PreferenceHelper
import mil.nga.giat.mage.sdk.utils.MediaUtility
import org.apache.commons.lang3.StringUtils
import org.json.JSONException
import org.json.JSONObject
import java.util.TreeMap
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
   @Inject lateinit var application: MageApplication
   @Inject lateinit var preferences: SharedPreferences
   @Inject lateinit var tokenProvider: TokenProvider
   @Inject lateinit var userLocalDataSource: UserLocalDataSource
   @Inject lateinit var cacheProvider: CacheProvider

   private lateinit var viewModel: LoginViewModel
   private lateinit var serverUrlText: TextView

   private var mOpenFilePath: String? = null
   private var mContinueSession = false

   public override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      val intent = intent
      mContinueSession = getIntent().getBooleanExtra(EXTRA_CONTINUE_SESSION, false)
      val continueSessionWhileUsing = getIntent().getBooleanExtra(
         EXTRA_CONTINUE_SESSION_WHILE_USING, false
      )
      intent.removeExtra(EXTRA_CONTINUE_SESSION_WHILE_USING)
      if (continueSessionWhileUsing && savedInstanceState == null) {
         showSessionExpiredDialog()
      }
      if (intent.getBooleanExtra("LOGOUT", false)) {
         application.onLogout(true)
      }

      // IMPORTANT: load the configuration from preferences files and server
      val preferenceHelper = PreferenceHelper.getInstance(applicationContext)
      preferenceHelper.initialize(false, xml::class.java)

      // check if the database needs to be upgraded, and if so log them out
      if (MageSqliteOpenHelper.DATABASE_VERSION != preferences.getInt(resources.getString(R.string.databaseVersionKey), 0)
      ) {
         application.onLogout(true)
      }
      preferences.edit().putInt(getString(R.string.databaseVersionKey), MageSqliteOpenHelper.DATABASE_VERSION).apply()

      // check google play services version
      val isGooglePlayServicesAvailable = GooglePlayServicesUtil.isGooglePlayServicesAvailable(
         applicationContext
      )
      if (isGooglePlayServicesAvailable != ConnectionResult.SUCCESS) {
         if (GooglePlayServicesUtil.isUserRecoverableError(isGooglePlayServicesAvailable)) {
            val dialog = GooglePlayServicesUtil.getErrorDialog(isGooglePlayServicesAvailable, this, 1)
            dialog?.setOnCancelListener { dialog1: DialogInterface ->
               dialog1.dismiss()
               finish()
            }
            dialog?.show()
         } else {
            AlertDialog.Builder(this).setTitle("Google Play Services")
               .setMessage("Google Play Services is not installed, or needs to be updated.  Please update Google Play Services before continuing.")
               .setPositiveButton(
                  android.R.string.ok
               ) { dialog, _ ->
                  dialog.dismiss()
                  finish()
               }.show()
         }
      }

      // Handle when MAGE was launched with a Uri (such as a local or remote cache file)
      var uri = intent.data
      if (uri == null) {
         val bundle = intent.extras
         if (bundle != null) {
            val objectUri = bundle[Intent.EXTRA_STREAM]
            if (objectUri != null) {
               uri = objectUri as Uri?
            }
         }
      }
      uri?.let { handleUri(it) }

      // if token is not expired, then skip the login module
      if (!tokenProvider.isExpired()) {
         skipLogin()
      } else {
         // temporarily prune complete work on every login to ensure our unique work is rescheduled
         WorkManager.getInstance(applicationContext).pruneWork()
         application.stopLocationService()
      }

      // no title bar
      setContentView(R.layout.activity_login)
      hideKeyboardOnClick(findViewById(R.id.login))
      (findViewById<View>(R.id.login_version) as TextView).text = "App Version: " + preferences.getString(getString(R.string.buildVersionKey), "NA")
      serverUrlText = findViewById(R.id.server_url)
      val serverUrl = preferences.getString(getString(R.string.serverURLKey), getString(R.string.serverURLDefaultValue))!!
      if (StringUtils.isEmpty(serverUrl)) {
         changeServerURL()
         return
      }
      findViewById<View>(R.id.server_url).setOnClickListener { changeServerURL() }
      serverUrlText.text = serverUrl

      // Setup login based on last api pull
      configureLogin()
      viewModel = ViewModelProvider(this).get(LoginViewModel::class.java)
      viewModel.apiStatus.observe(this) { observeApi() }
      viewModel.authenticationState.observe(this) { observeAuthenticationState(it) }
      viewModel.authenticationStatus.observe(this) { observeAuthentication(it) }
      viewModel.authorizationStatus.observe(this) { observeAuthorization(it) }
      viewModel.api(serverUrl)
   }

   override fun onBackPressed() {
      if (mContinueSession) {
         // In this case the activity stack was preserved. Don't allow the user to go back to an activity without logging in.
         // Since this is the application entry point, assume back means go home.
         val intent = Intent(Intent.ACTION_MAIN)
         intent.addCategory(Intent.CATEGORY_HOME)
         intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
         startActivity(intent)
         return
      }
      super.onBackPressed()
   }

   private fun observeApi() {
      configureLogin()
   }

   private fun observeAuthenticationState(state: AuthenticationState) {
      findViewById<View>(R.id.progress).visibility = if (state === AuthenticationState.LOADING) {
         View.VISIBLE
      }  else  {
         View.VISIBLE
      }
   }

   private fun observeAuthentication(authentication: Authentication?) {
      if (authentication == null) return
      val status = authentication.status
      if (status is AuthenticationStatus.Success) {
         val token = status.token
         viewModel.authorize(authentication.strategy, token)
      } else if (status is AccountCreated) {
         val message = status.message
         val dialog = ContactDialog(this, (preferences), "Account Created", message)
         dialog.setAuthenticationStrategy(authentication.strategy)
         dialog.show(null)
      } else if (status is Offline) {
         val message = status.message
         val dialog = ContactDialog(this, (preferences), "Sign in Failed", message)
         dialog.setAuthenticationStrategy(authentication.strategy)
         dialog.show { workOffline: Boolean ->
            if (workOffline) {
               loginComplete(false)
            }
            viewModel.completeOffline(workOffline)
         }
      } else if (status is AuthenticationStatus.Failure) {
         val message = status.message
         val dialog = ContactDialog(this, (preferences), "Sign in Failed", message)
         dialog.setAuthenticationStrategy(authentication.strategy)
         dialog.show(null)
      }
   }

   private fun observeAuthorization(authorization: Authorization?) {
      if (authorization == null) return
      val status = authorization.status
      if (status is AuthorizationStatus.Success) {
         loginComplete(status.sessionChanged)
      } else if (status is FailAuthorization) {
         val dialog = ContactDialog(
            this,
            preferences,
            "Registration Sent",
            getString(R.string.device_registered_text)
         )
         val user = status.user
         if (user != null) {
            dialog.username = user.username
         }
         dialog.show(null)
      } else if (status is FailInvalidServer) {
         val dialog = ContactDialog(
            this,
            preferences,
            "Application Compatibility Error",
            "MAGE is not compatible with this server, please ensure your application is up to date or contact your MAGE administrator."
         )
         dialog.show(null)
      }
      if (status is FailAuthentication) {
         val message = status.message
         val dialog = ContactDialog(this, preferences, "Sign-in Failed", message)
         val user = status.user
         if (user != null) {
            dialog.username = user.username
         }
         dialog.show(null)
      }
   }

   private fun configureLogin() {
      val transaction = supportFragmentManager.beginTransaction()
      val strategies: MutableMap<String?, JSONObject> = TreeMap()

      // TODO marshal authentication strategies to POJOs with Jackson
      val authenticationStrategies =
         PreferenceHelper.getInstance(applicationContext).authenticationStrategies
      val iterator = authenticationStrategies.keys()
      while (iterator.hasNext()) {
         val strategyKey = iterator.next()
         try {
            val strategy = authenticationStrategies[strategyKey] as JSONObject
            if (("local" == strategyKey)) {
               strategy.putOpt("type", strategyKey)
            }
            strategies[strategyKey] = strategy
         } catch (e: JSONException) {
            Log.e(LOG_NAME, "Error parsing authentication strategy", e)
         }
      }
      if (strategies.size > 1 && strategies.containsKey("local")) {
         findViewById<View>(R.id.or).visibility = View.VISIBLE
      } else {
         findViewById<View>(R.id.or).visibility = View.GONE
      }
      findViewById<View>(R.id.google_login_button).visibility = View.GONE
      for (entry: Map.Entry<String?, JSONObject> in strategies.entries) {
         val authenticationName = entry.key
         val authenticationType = entry.value.optString("type")
         if (supportFragmentManager.findFragmentByTag(authenticationName) != null) continue
         if (("local" == authenticationName)) {
            val loginFragment: Fragment = MageLoginFragment.newInstance(
               entry.key!!, entry.value
            )
            transaction.add(R.id.local_auth, loginFragment, authenticationName)
         } else if (("oauth" == authenticationType)) {
            val loginFragment: Fragment = IdpLoginFragment.newInstance(
               (entry.key)!!, entry.value
            )
            transaction.add(R.id.third_party_auth, loginFragment, authenticationName)
         } else if (("saml" == authenticationType)) {
            val loginFragment: Fragment = IdpLoginFragment.newInstance(
               (entry.key)!!, entry.value
            )
            transaction.add(R.id.third_party_auth, loginFragment, authenticationName)
         } else if (("ldap" == authenticationType)) {
            val loginFragment: Fragment = LdapLoginFragment.newInstance(
               (entry.key)!!, entry.value
            )
            transaction.add(R.id.third_party_auth, loginFragment, authenticationName)
         } else {
            val loginFragment: Fragment = IdpLoginFragment.newInstance(
               (entry.key)!!, entry.value
            )
            transaction.add(R.id.third_party_auth, loginFragment, authenticationName)
         }
      }

      // Remove authentication fragments that have been removed from server
      for (fragment: Fragment in supportFragmentManager.fragments) {
         if (!strategies.keys.contains(fragment.tag)) {
            transaction.remove(fragment)
         }
      }
      transaction.commit()
   }

   /**
    * Hides keyboard when clicking elsewhere
    *
    * @param view
    */
   private fun hideKeyboardOnClick(view: View) {
      // Set up touch listener for non-text box views to hide keyboard.
      if (view !is EditText && view !is Button) {
         view.setOnTouchListener { _, _ ->
            view.performClick()
            val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            if (currentFocus != null) {
               inputMethodManager.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
            }
            false
         }
      }

      // If a layout container, iterate over children and seed recursion.
      if (view is ViewGroup) {
         for (i in 0 until view.childCount) {
            val innerView = view.getChildAt(i)
            hideKeyboardOnClick(innerView)
         }
      }
   }

   private fun changeServerURL() {
      val intent = Intent(this, ServerUrlActivity::class.java)
      startActivity(intent)
      finish()
   }

   /**
    * Handle the Uri used to launch MAGE
    *
    * @param uri
    */
   private fun handleUri(uri: Uri) {
      // Attempt to get a local file path
      val openPath = MediaUtility.getPath(this, uri)

      // If not a local or temporary file path, copy the file to cache
      // Cannot pass this to another activity to handle as the URI might
      // become invalid between now and then.  Copy it now
      if (openPath == null || MediaUtility.isTemporaryPath(openPath)) {
         CoroutineScope(Dispatchers.IO).launch {
            CacheUtils(applicationContext, cacheProvider).copyToCache(uri, openPath)
         }

      } else {
         // Else, store the path to pass to further intents
         mOpenFilePath = openPath
      }
   }

   /**
    * Fired when user clicks signup
    */
   fun signup(view: View?) {
      val intent = if (isServerVersion5(applicationContext)) {
         Intent(applicationContext, SignupActivityServer5::class.java)
      } else {
         Intent(applicationContext, SignupActivity::class.java)
      }
      startActivity(intent)
      finish()
   }

   private fun loginComplete(userChanged: Boolean) {
      val preserveActivityStack = !userChanged && mContinueSession
      startNextActivityAndFinish(preserveActivityStack)
   }

   private fun startNextActivityAndFinish(preserveActivityStack: Boolean) {
      // Continue session if there are other activities on the stack
      if (preserveActivityStack && !isTaskRoot) {
         // We are going to return user to the app where they last left off,
         // make sure to start up MAGE services
         application.onLogin()

         // TODO look at refreshing the event here...
      } else {
         val showDisclaimer = preferences.getBoolean(getString(R.string.serverDisclaimerShow), false)
         val intent = if (showDisclaimer) Intent(
            applicationContext,
            DisclaimerActivity::class.java
         ) else Intent(
            applicationContext, EventsActivity::class.java
         )

         // If launched with a local file path, save as an extra
         if (mOpenFilePath != null) {
            intent.putExtra(LandingActivity.EXTRA_OPEN_FILE_PATH, mOpenFilePath)
         }
         startActivity(intent)
      }
      finish()
   }

   private fun skipLogin() {
      val intent: Intent
      val disclaimerAccepted = preferences.getBoolean(getString(R.string.disclaimerAcceptedKey), false)
      if (disclaimerAccepted) {
         var event: Event? = null
         val user = userLocalDataSource.readCurrentUser()
         if (user != null) {
            event = user.currentEvent
         }
         intent =
            if (event == null) Intent(applicationContext, EventsActivity::class.java) else Intent(
               applicationContext, LandingActivity::class.java
            )
      } else {
         intent = Intent(applicationContext, DisclaimerActivity::class.java)
      }

      // If launched with a local file path, save as an extra
      if (mOpenFilePath != null) {
         intent.putExtra(LandingActivity.EXTRA_OPEN_FILE_PATH, mOpenFilePath)
      }
      startActivity(intent)
      finish()
   }

   override fun onResume() {
      super.onResume()
      if (intent.getBooleanExtra("LOGOUT", false)) {
         application.onLogout(true)
      }
   }

   private fun showSessionExpiredDialog() {
      val dialog = AlertDialog.Builder(this)
         .setTitle("Session Expired")
         .setCancelable(false)
         .setMessage("We apologize, but it looks like your MAGE session has expired.  Please login and we will take you back to what you were doing.")
         .setPositiveButton(android.R.string.ok, null).create()
      dialog.setCanceledOnTouchOutside(false)
      dialog.show()
   }

   companion object {
      private val LOG_NAME = LoginActivity::class.java.name

      const val EXTRA_CONTINUE_SESSION = "CONTINUE_SESSION"
      const val EXTRA_CONTINUE_SESSION_WHILE_USING = "CONTINUE_SESSION_WHILE_USING"
   }
}