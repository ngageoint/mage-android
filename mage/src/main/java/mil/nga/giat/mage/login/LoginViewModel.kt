package mil.nga.giat.mage.login

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import mil.nga.giat.mage.R
import mil.nga.giat.mage.dagger.module.ApplicationContext
import mil.nga.giat.mage.sdk.datastore.DaoStore
import mil.nga.giat.mage.sdk.datastore.user.User
import mil.nga.giat.mage.sdk.datastore.user.UserHelper
import mil.nga.giat.mage.sdk.login.AuthenticationStatus
import mil.nga.giat.mage.sdk.login.AuthenticationTask
import mil.nga.giat.mage.sdk.login.AuthorizationStatus
import mil.nga.giat.mage.sdk.login.AuthorizationTask
import mil.nga.giat.mage.sdk.preferences.ServerApi
import mil.nga.giat.mage.sdk.utils.PasswordUtility
import org.apache.commons.lang3.StringUtils
import java.util.*
import javax.inject.Inject

class LoginViewModel @Inject constructor(
    @ApplicationContext val context: Context,
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

    private var currentUsername: String? = null
    private var localCredentials: Array<String>? = null

    private val _authenticationState = MutableLiveData<AuthenticationState>()
    val authenticationState: LiveData<AuthenticationState> = _authenticationState

    private val _authenticationStatus = MutableLiveData<Authentication>()
    val authenticationStatus: LiveData<Authentication> = _authenticationStatus

    fun authenticate(strategy: String, credentials: Array<String>, allowDisconnectedLogin: Boolean = false) {
        AuthenticationTask(context, allowDisconnectedLogin) {
            _authenticationStatus.value = Authentication(strategy, it)

            if (strategy == "local" && it.status == AuthenticationStatus.Status.SUCCESSFUL_AUTHENTICATION) {
                localCredentials = credentials
            }

            _authenticationState.value = if (it.status != AuthenticationStatus.Status.FAILED_AUTHENTICATION) {
                AuthenticationState.SUCCESS
            } else {
                AuthenticationState.ERROR
            }
        }.execute(*credentials)

        _authenticationStatus.value = null
        _authenticationState.value = AuthenticationState.LOADING
    }

    private val _authorizationStatus = MutableLiveData<Authorization>()
    val authorizationStatus: LiveData<Authorization> = _authorizationStatus

    fun authorize(strategy: String, token: String, status: AuthenticationStatus? = null) {
        AuthorizationTask(context) {
            if (it.status == AuthorizationStatus.Status.SUCCESSFUL_AUTHORIZATION) {
                if ("local" == strategy) {
                    setupDisconnectedLogin()
                }

                val userChanged = completeAuthorization(strategy, it.user)
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

        val serverApi = ServerApi(context)
        serverApi.validateServerApi(url) { valid, _ ->
            if (authenticationState.value != AuthenticationState.LOADING) {
                _apiStatus.value = valid
            }
        }

        _apiStatus.value = null
    }

    private fun setupDisconnectedLogin() {
        localCredentials?.let {
            val username = it[0]
            val password = it[0]
            val editor = preferences.edit()
            editor.putString(context.getString(R.string.usernameKey), username).apply()
            try {
                val hashedPassword = PasswordUtility.getSaltedHash(password)
                editor.putString(context.getString(R.string.passwordHashKey), hashedPassword).commit()
            } catch (e: Exception) {
                Log.e(LOG_NAME, "Could not hash password", e)
            }
        }

        localCredentials = null
    }

    private fun completeAuthorization(strategy: String, user: User): Boolean {
        val previousUser = preferences.getString(context.getString(R.string.sessionUserKey), null)
        val previousStrategy = preferences.getString(context.getString(R.string.sessionStrategyKey), null)
        val userChanged = (strategy != previousStrategy) || (user.username != previousUser)

        if (userChanged) {
            DaoStore.getInstance(context).resetDatabase()
        }

        user.fetchedDate = Date()
        val userHelper = UserHelper.getInstance(context)
        val currentUser = userHelper.createOrUpdate(user)
        userHelper.setCurrentUser(currentUser)

        preferences.edit()
            .putString(context.getString(R.string.sessionUserKey), user.username)
            .putString(context.getString(R.string.sessionStrategyKey), strategy)
            .apply()

        return userChanged
    }
}