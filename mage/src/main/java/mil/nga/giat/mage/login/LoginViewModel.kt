package mil.nga.giat.mage.login

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import mil.nga.giat.mage.R
import mil.nga.giat.mage.sdk.datastore.DaoStore
import mil.nga.giat.mage.sdk.datastore.user.UserHelper
import mil.nga.giat.mage.sdk.login.AuthenticationStatus
import mil.nga.giat.mage.sdk.login.AuthenticationTask
import mil.nga.giat.mage.sdk.login.AuthorizationStatus
import mil.nga.giat.mage.sdk.login.AuthorizationTask
import mil.nga.giat.mage.sdk.preferences.PreferenceHelper
import mil.nga.giat.mage.sdk.preferences.ServerApi
import mil.nga.giat.mage.sdk.utils.ISO8601DateFormatFactory
import mil.nga.giat.mage.sdk.utils.PasswordUtility
import org.apache.commons.lang3.StringUtils
import java.util.*
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    val application: Application,
    val preferences: SharedPreferences
): ViewModel() {

    companion object {
        private val LOG_NAME = LoginViewModel::class.java.name
    }

    data class Authentication(val strategy: String, val status: AuthenticationStatus)
    data class Authorization(val status: AuthorizationStatus, val userChanged: Boolean = true)

    enum class AuthenticationState {
        SUCCESS, ERROR, LOADING
    }

    private var localCredentials: Array<String>? = null

    private val _authenticationState = MutableLiveData<AuthenticationState>()
    val authenticationState: LiveData<AuthenticationState> = _authenticationState

    private val _authenticationStatus = MutableLiveData<Authentication?>()
    val authenticationStatus: LiveData<Authentication?> = _authenticationStatus

    fun authenticate(strategy: String, credentials: Array<String>, allowDisconnectedLogin: Boolean = false) {
        AuthenticationTask(application, allowDisconnectedLogin) {
            _authenticationStatus.value = Authentication(strategy, it)

            if (strategy == "local" && it.status == AuthenticationStatus.Status.SUCCESSFUL_AUTHENTICATION) {
                localCredentials = credentials
            }

            _authenticationState.value = if (it.status != AuthenticationStatus.Status.FAILED_AUTHENTICATION) {
                AuthenticationState.SUCCESS
            } else {
                AuthenticationState.ERROR
            }
        }.execute(*credentials, strategy)

        _authenticationStatus.value = null
        _authenticationState.value = AuthenticationState.LOADING
    }

    private val _authorizationStatus = MutableLiveData<Authorization?>()
    val authorizationStatus: LiveData<Authorization?> = _authorizationStatus

    fun authorize(strategy: String, token: String) {
        AuthorizationTask(application) {
            if (it.status == AuthorizationStatus.Status.SUCCESSFUL_AUTHORIZATION) {
                if ("local" == strategy) {
                    setupDisconnectedLogin()
                }

                val userChanged = completeAuthorization(strategy, it)
                _authorizationStatus.value = Authorization(it, userChanged)

            } else {
                _authorizationStatus.value = Authorization(it)
            }

            _authenticationState.value = if (it.status == AuthorizationStatus.Status.SUCCESSFUL_AUTHORIZATION) {
                AuthenticationState.SUCCESS
            } else {
                AuthenticationState.ERROR
            }
        }.execute(token)

        _authorizationStatus.value = null
        _authenticationState.value = AuthenticationState.LOADING
    }

    private val _apiStatus = MutableLiveData<Boolean>()
    val apiStatus: LiveData<Boolean> = _apiStatus

    fun api(url: String) {
        if (StringUtils.isEmpty(url)) {
            return
        }

        val serverApi = ServerApi(application)
        serverApi.validateServerApi(url) { valid, _ ->
            if (authenticationState.value != AuthenticationState.LOADING) {
                _apiStatus.value = valid
            }
        }
    }

    private fun setupDisconnectedLogin() {
        localCredentials?.let {
            val username = it[0]
            val password = it[1]
            val editor = preferences.edit()
            editor.putString(application.getString(R.string.usernameKey), username).apply()
            try {
                val hashedPassword = PasswordUtility.getSaltedHash(password)
                editor.putString(application.getString(R.string.passwordHashKey), hashedPassword).commit()
            } catch (e: Exception) {
                Log.e(LOG_NAME, "Could not hash password", e)
            }
        }

        localCredentials = null
    }

    private fun completeAuthorization(strategy: String, status: AuthorizationStatus): Boolean {
        val user = status.user;
        val previousUser = preferences.getString(application.getString(R.string.sessionUserKey), null)
        val previousStrategy = preferences.getString(application.getString(R.string.sessionStrategyKey), null)
        val sessionChanged =
            (previousStrategy != null && strategy != previousStrategy) ||
            (previousUser != null && user.username != previousUser)

        if (sessionChanged) {
            DaoStore.getInstance(application).resetDatabase()

            val preferenceHelper = PreferenceHelper.getInstance(application)
            preferenceHelper.initialize(true, R.xml::class.java)

            val dayNightTheme = preferences.getInt(application.resources.getString(R.string.dayNightThemeKey), application.resources.getInteger(R.integer.dayNightThemeDefaultValue))
            AppCompatDelegate.setDefaultNightMode(dayNightTheme)
        }

        user.fetchedDate = Date()
        val userHelper = UserHelper.getInstance(application)
        val currentUser = userHelper.createOrUpdate(user)
        userHelper.setCurrentUser(currentUser)

        // Successful login, put the token information in the shared preferences
        preferences.edit()
           .putString(application.getString(R.string.sessionUserKey), user.username)
           .putString(application.getString(R.string.sessionStrategyKey), strategy)
           .putString(application.getString(R.string.tokenKey), status.token.trim { it <= ' ' })
           .putString(application.getString(R.string.tokenExpirationDateKey), ISO8601DateFormatFactory.ISO8601().format(status.tokenExpiration))
           .putLong(application.getString(R.string.tokenExpirationLengthKey), status.tokenExpiration.time - Date().time)
           .commit()

        return sessionChanged
    }
}