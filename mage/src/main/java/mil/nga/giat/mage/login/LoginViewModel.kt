package mil.nga.giat.mage.login

import android.app.Application
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mil.nga.giat.mage.R
import mil.nga.giat.mage.data.repository.api.ApiRepository
import mil.nga.giat.mage.data.repository.api.ApiResponse
import mil.nga.giat.mage.data.repository.user.UserRepository
import mil.nga.giat.mage.di.TokenProvider
import mil.nga.giat.mage.database.dao.MageSqliteOpenHelper
import mil.nga.giat.mage.sdk.preferences.PreferenceHelper
import mil.nga.giat.mage.sdk.utils.PasswordUtility
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
   val application: Application,
   val preferences: SharedPreferences,
   private val daoStore: MageSqliteOpenHelper,
   private val tokenProvider: TokenProvider,
   private val apiRepository: ApiRepository,
   private val userRepository: UserRepository
): ViewModel() {

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

    fun authenticate(strategy: String, username: String, password: String) {
        _authenticationStatus.value = null
        _authenticationState.value = AuthenticationState.LOADING

        viewModelScope.launch(Dispatchers.IO) {
            val status = userRepository.authenticateLocal(strategy, username, password)
            _authenticationStatus.postValue(Authentication(strategy, status))

            if (strategy == "local" && status is AuthenticationStatus.Success) {
                localCredentials = arrayOf(username, password)
            }

            val state = if (status !is AuthenticationStatus.Failure) {
                AuthenticationState.SUCCESS
            } else AuthenticationState.ERROR
            _authenticationState.postValue(state)
        }
    }

    private val _authorizationStatus = MutableLiveData<Authorization?>()
    val authorizationStatus: LiveData<Authorization?> = _authorizationStatus

    fun authorize(strategy: String, token: String) {
        _authorizationStatus.value = null
        _authenticationState.value = AuthenticationState.LOADING

        viewModelScope.launch {
            when (val status = userRepository.authorize(token)) {
                is AuthorizationStatus.Success -> {
                    if ("local" == strategy) {
                        setupDisconnectedLogin()
                    }

                    val userChanged = completeAuthorization(strategy, status)
                    _authorizationStatus.value = Authorization(status, userChanged)
                    _authenticationState.value = AuthenticationState.SUCCESS
                }
                else -> {
                    _authorizationStatus.value = Authorization(status)
                    _authenticationState.value = AuthenticationState.ERROR
                }
            }
        }
    }

    fun completeOffline(workOffline: Boolean) {
        if (workOffline) {
            userRepository.authenticateOffline()
        }

        _authenticationState.postValue(AuthenticationState.ERROR)
    }

    private val _apiStatus = MutableLiveData<Boolean>()
    val apiStatus: LiveData<Boolean> = _apiStatus

    fun api(url: String) {
        if (url.isNotEmpty()) {
            viewModelScope.launch {
                val response = apiRepository.getApi(url)
                if (authenticationState.value != AuthenticationState.LOADING) {
                    _apiStatus.value = response is ApiResponse.Valid
                }
            }
        }
    }

    private fun setupDisconnectedLogin() {
        localCredentials?.let { (username, password) ->
            val editor = preferences.edit()
            editor.putString(application.getString(R.string.usernameKey), username).apply()
            val hashedPassword = PasswordUtility.getSaltedHash(password)
            editor.putString(application.getString(R.string.passwordHashKey), hashedPassword).commit()
        }

        localCredentials = null
    }

    private fun completeAuthorization(strategy: String, status: AuthorizationStatus.Success): Boolean {
        val previousUser = preferences.getString(application.getString(R.string.sessionUserKey), null)
        val previousStrategy = preferences.getString(application.getString(R.string.sessionStrategyKey), null)
        val sessionChanged =
            (previousStrategy != null && strategy != previousStrategy) ||
            (previousUser != null && status.user.username != previousUser)

        if (sessionChanged) {
            daoStore.resetDatabase()

            val preferenceHelper = PreferenceHelper.getInstance(application)
            preferenceHelper.initialize(true, R.xml::class.java)

            val dayNightTheme = preferences.getInt(application.resources.getString(R.string.dayNightThemeKey), application.resources.getInteger(R.integer.dayNightThemeDefaultValue))
            AppCompatDelegate.setDefaultNightMode(dayNightTheme)
        }

        tokenProvider.updateToken(
            username = status.user.username,
            authenticationStrategy = strategy,
            token = status.token.trim(),
            expiration = status.tokenExpiration
        )

        return sessionChanged
    }
}