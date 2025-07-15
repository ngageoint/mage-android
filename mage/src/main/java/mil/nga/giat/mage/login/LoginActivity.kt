package mil.nga.giat.mage.login

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.work.WorkManager
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
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
import mil.nga.giat.mage.login.LoginViewModel.Authorization
import mil.nga.giat.mage.login.idp.IdpLoginActivity
import mil.nga.giat.mage.map.cache.CacheProvider
import mil.nga.giat.mage.sdk.Compatibility.Companion.isServerVersion5
import mil.nga.giat.mage.sdk.preferences.PreferenceHelper
import mil.nga.giat.mage.sdk.utils.MediaUtility
import mil.nga.giat.mage.ui.login.LoginScreen
import mil.nga.giat.mage.utils.IntentConstants
import org.apache.commons.lang3.StringUtils
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
   @Inject lateinit var application: MageApplication
   @Inject lateinit var preferences: SharedPreferences
   @Inject lateinit var tokenProvider: TokenProvider
   @Inject lateinit var userLocalDataSource: UserLocalDataSource
   @Inject lateinit var cacheProvider: CacheProvider

   private lateinit var viewModel: LoginViewModel

   private var mOpenFilePath: String? = null
   private var mContinueSession = false

   private lateinit var idpLoginLauncher: ActivityResultLauncher<Intent>

   public override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      val serverUrl = preferences.getString(getString(R.string.serverURLKey), getString(R.string.serverURLDefaultValue))!!

      //if serverUrl is not populated, then the user needs to specify one prior to login
      if (StringUtils.isEmpty(serverUrl)) {
         changeServerURL()
      } else {
         // if token is not expired, then skip the login module
         if (!tokenProvider.isExpired()) {
            skipLogin()
         } else {
            // temporarily prune complete work on every login to ensure our unique work is rescheduled
            WorkManager.getInstance(applicationContext).pruneWork()
            application.stopLocationService()

            val intent = intent
            mContinueSession = getIntent().getBooleanExtra(EXTRA_CONTINUE_SESSION, false)

            val continueSessionWhileUsing =
               getIntent().getBooleanExtra(EXTRA_CONTINUE_SESSION_WHILE_USING, false)

            intent.removeExtra(EXTRA_CONTINUE_SESSION_WHILE_USING)
            if (continueSessionWhileUsing && savedInstanceState == null) {
               showSessionExpiredDialog()
            }

            val version = "App Version: " + preferences.getString(getString(R.string.buildVersionKey), "NA")

            // IMPORTANT: load the configuration from preferences files and server
            val preferenceHelper = PreferenceHelper.getInstance(applicationContext)
            preferenceHelper.initialize(false, xml::class.java)

            // check if the database needs to be upgraded, and if so log them out
            if (MageSqliteOpenHelper.DATABASE_VERSION != preferences.getInt(
                  resources.getString(R.string.databaseVersionKey), 0)) {
               application.onLogout(true)
            } else if (intent.getBooleanExtra("LOGOUT", false)) {
               application.onLogout(true)
            }

            preferences.edit().putInt(getString(R.string.databaseVersionKey), MageSqliteOpenHelper.DATABASE_VERSION).apply()

            //check Google Play version - a minimum version is required to use the app
            checkGooglePlay()

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

            //launches web login page hosted outside the app for IDP auth (SAML, OPENIDCONNECT, OAUTH)
            val onIdpLoginClick = { authType: ServerAuthTypes ->
               val idpLoginIntent = IdpLoginActivity.intent(this, serverUrl, authType)
               idpLoginLauncher.launch(idpLoginIntent)
            }

            //callback handler from IdpLoginActivity
            idpLoginLauncher =
               registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                  if (result.resultCode == Activity.RESULT_OK) {
                     val idpResponseToken = result.data?.getStringExtra(IdpLoginActivity.EXTRA_IDP_TOKEN)
                     val upperCaseStrategyName = result.data?.getStringExtra(IdpLoginActivity.EXTRA_IDP_STRATEGY)?.uppercase()

                     val authType = ServerAuthTypes.entries.find { it.name == upperCaseStrategyName }
                     if (authType != null) {
                        viewModel.authorize(authType, idpResponseToken ?: "")
                     } else {
                        Log.e(LOG_NAME, "IDP Login Failed or Cancelled")
                        processAuthenticationResult(Authentication(ServerAuthTypes.SAML, AuthenticationStatus.Failure(0, "Login failed or cancelled")))
                     }
                  } else {
                     Log.e(LOG_NAME, "IDP Login Failed or Cancelled")
                     processAuthenticationResult(Authentication(ServerAuthTypes.SAML, AuthenticationStatus.Failure(0, "Login failed or cancelled")))
                  }
               }

            viewModel = ViewModelProvider(this).get(LoginViewModel::class.java)

            //retrieve previously stored authStrategies JSON and launch call to refresh it
            viewModel.availableLoginTypesJSON.value = PreferenceHelper.getInstance(applicationContext).authenticationStrategies
            viewModel.checkApi(serverUrl)

            //server authentication for LOCAL and LDAP auth
            val authenticate = {
               authType: ServerAuthTypes, userId: String, pwd: String ->
                  viewModel.authenticate(authType, userId.lowercase(), pwd)
            }

            setContent {
               LoginScreen(
                  viewModel.availableLoginTypesJSON,
                  viewModel.showProgressSpinner,
                  onServerUrlClick = { changeServerURL(true) },
                  onLoginClick = authenticate,
                  onIdpLoginClick = onIdpLoginClick,
                  onSignUpClick = { signup() },
                  serverUrl = serverUrl,
                  version = version
               )
            }

            //collector for authentication and authorization events
            collectAuthEvents()
         }
      }
   }

   //collect auth events fired from LoginViewModel
   private fun collectAuthEvents() {
      lifecycleScope.launch {
         //repeatOnLifecycle ensures collection stops when view is paused/stopped, and restarts when resumed
         repeatOnLifecycle(Lifecycle.State.STARTED) {

            //monitor auth state to control display of progress spinner
            launch {
               viewModel.authenticationProcessState.collectLatest { state ->
                  updateProgressSpinner(state)
               }
            }

            //process the authentication result event
            launch {
               viewModel.authenticationResultEvents.collectLatest { authentication ->
                  processAuthenticationResult(authentication)
               }
            }

            //process the authorization result event
            launch {
               viewModel.authorizationResultEvents.collectLatest { authorization ->
                  processAuthorizationResult(authorization)
               }
            }

            //process the "api" result event to update available login options based on server config
            launch {
               viewModel.apiStatusSuccessEvent.collectLatest {
                  updateLoginOptions()
               }
            }
         }
      }
   }

   //check google play services version
   private fun checkGooglePlay() {
      val isGooglePlayServicesAvailable =
         GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(applicationContext)
      if (isGooglePlayServicesAvailable != ConnectionResult.SUCCESS) {
         if (GoogleApiAvailability.getInstance()
               .isUserResolvableError(isGooglePlayServicesAvailable)
         ) {
            val dialog = GoogleApiAvailability.getInstance()
               .getErrorDialog(this, 1, isGooglePlayServicesAvailable)
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

   private fun updateProgressSpinner(state: AuthenticationState) {
      if (state === AuthenticationState.LOADING) {
         viewModel.showProgressSpinner.value = true
      } else {
         viewModel.showProgressSpinner.value = false
      }
   }

   private fun processAuthenticationResult(authentication: Authentication?) {
      if (authentication == null) return
      when (val status = authentication.status){
         is AuthenticationStatus.Success -> {
            val token = status.token
            viewModel.authorize(authentication.authType, token)
         }
         is AccountCreated -> {
            val message = status.message
            val dialog = ContactDialog(this, (preferences), "Account Created", message)
            dialog.setAuthenticationStrategy(authentication.authType.name)
            dialog.show(null)
         }
         is Offline -> {
            val message = status.message
            val dialog = ContactDialog(this, (preferences), "Sign in Failed", message)
            dialog.setAuthenticationStrategy(authentication.authType.name)
            dialog.show { workOffline: Boolean ->
               if (workOffline) {
                  loginComplete(false)
               }
               viewModel.completeOffline(workOffline)
            }
         }
         is AuthenticationStatus.Failure -> {
            val message = status.message
            val dialog = ContactDialog(this, (preferences), "Sign in Failed", message)
            dialog.setAuthenticationStrategy(authentication.authType.name)
            dialog.show(null)
         }
      }
   }

   private fun processAuthorizationResult(authorization: Authorization?) {
      if (authorization == null) return
      when (val status = authorization.status) {
         is AuthorizationStatus.Success -> {
            loginComplete(status.sessionChanged)
         }
         is FailAuthorization -> {
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
         }
         is FailInvalidServer -> {
            val dialog = ContactDialog(
               this,
               preferences,
               "Application Compatibility Error",
               "MAGE is not compatible with this server, please ensure your application is up to date or contact your MAGE administrator."
            )
            dialog.show(null)
         }
         is FailAuthentication -> {
            val message = status.message
            val dialog = ContactDialog(this, preferences, "Sign-in Failed", message)
            val user = status.user
            if (user != null) {
               dialog.username = user.username
            }
            dialog.show(null)
         }
      }
   }

   private fun updateLoginOptions() {
      viewModel.availableLoginTypesJSON.value = PreferenceHelper.getInstance(applicationContext).authenticationStrategies
   }


   private fun changeServerURL(launchedFromButtonClick: Boolean = false) {
      val intent = Intent(this, ServerUrlActivity::class.java)
      intent.putExtra(IntentConstants.LAUNCHED_FROM_BUTTON_CLICK, launchedFromButtonClick)
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
   fun signup() {
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