package mil.nga.giat.mage.login

import android.app.Application
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mil.nga.giat.mage.R
import mil.nga.giat.mage.data.repository.api.ApiRepository
import mil.nga.giat.mage.data.repository.api.ApiResponse
import mil.nga.giat.mage.data.repository.user.UserRepository
import mil.nga.giat.mage.sdk.utils.PasswordUtility
import org.apache.commons.lang3.StringUtils
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
   val application: Application,
   val preferences: SharedPreferences,
   private val apiRepository: ApiRepository,
   private val userRepository: UserRepository
): ViewModel() {

    data class Authentication(val authType: ServerAuthTypes, val status: AuthenticationStatus)
    data class Authorization(val status: AuthorizationStatus)
    data class LocalCredentials(val username: String, val pwd: String)

    private var localCredentials: LocalCredentials? = null

    private val _authenticationProcessState = MutableStateFlow(AuthenticationState.IDLE)
    val authenticationProcessState: StateFlow<AuthenticationState> = _authenticationProcessState.asStateFlow()

    private val _authenticationResultEvents = MutableSharedFlow<Authentication>()
    val authenticationResultEvents: SharedFlow<Authentication> = _authenticationResultEvents.asSharedFlow()

    private val _authorizationResultEvents = MutableSharedFlow<Authorization>()
    val authorizationResultEvents: SharedFlow<Authorization> = _authorizationResultEvents.asSharedFlow()

    private val _apiStatusSuccessEvent = MutableSharedFlow<Boolean>()
    val apiStatusSuccessEvent: SharedFlow<Boolean> = _apiStatusSuccessEvent.asSharedFlow()

    val availableLoginTypesJSON = mutableStateOf(JSONObject())
    val showProgressSpinner = mutableStateOf(false)

    fun authenticate(authType: ServerAuthTypes, username: String, password: String) {
        _authenticationProcessState.value = AuthenticationState.LOADING

        viewModelScope.launch(Dispatchers.IO) {
            val status = userRepository.authenticateLocal(authType.name.lowercase(), username, password)

            val authResult = Authentication(authType, status)
            _authenticationResultEvents.emit(authResult)

            if (StringUtils.equalsIgnoreCase(authType.name, ServerAuthTypes.LOCAL.name) && status is AuthenticationStatus.Success) {
                localCredentials = LocalCredentials(username, password)
            }

            val state = if (status !is AuthenticationStatus.Failure) {
                AuthenticationState.SUCCESS
            } else {
                AuthenticationState.ERROR
            }

            _authenticationProcessState.value = state
        }
    }

    fun authorize(authType: ServerAuthTypes, token: String) {
        _authenticationProcessState.value = AuthenticationState.LOADING

        viewModelScope.launch {
            val status = userRepository.authorize(authType.name.lowercase(), token)
            val authenticationState =
                if (status is AuthorizationStatus.Success) {
                    if (StringUtils.equalsIgnoreCase(authType.name, ServerAuthTypes.LOCAL.name)) {
                        setupDisconnectedLoginForLocal()
                    }
                    AuthenticationState.SUCCESS
                } else {
                    AuthenticationState.ERROR
                }

            val authorizationStatus = Authorization(status)

            _authorizationResultEvents.emit(authorizationStatus)
            _authenticationProcessState.value = authenticationState
        }
    }

    fun checkApi(url: String) {
        if (url.isNotEmpty()) {
            viewModelScope.launch {
                val response = apiRepository.getApi(url)

                if (authenticationProcessState.value != AuthenticationState.LOADING) {
                    if (response is ApiResponse.Success) {
                        _apiStatusSuccessEvent.emit(true)
                    }
                }
            }
        }
    }

    fun completeOffline(workOffline: Boolean) {
        if (workOffline) {
            userRepository.authenticateOffline()
        }

        _authenticationProcessState.value = AuthenticationState.ERROR
    }

    private fun setupDisconnectedLoginForLocal() {
        localCredentials?.let {
            val editor = preferences.edit()
            editor.putString(application.getString(R.string.usernameKey), it.username).apply()
            val hashedPassword = PasswordUtility.getSaltedHash(it.pwd)
            editor.putString(application.getString(R.string.passwordHashKey), hashedPassword).commit()
        }

        localCredentials = null
    }
}