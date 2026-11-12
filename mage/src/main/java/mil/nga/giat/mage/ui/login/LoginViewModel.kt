package mil.nga.giat.mage.ui.login

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import mil.nga.giat.mage.data.datasource.authentication.AuthenticationStrategy
import mil.nga.giat.mage.data.datasource.authentication.AuthenticationStrategyType
import mil.nga.giat.mage.data.repository.preferences.PreferencesRepository
import javax.inject.Inject

class AuthenticationState(
    val local: AuthenticationStrategy? = null,
    val identityProviders: List<AuthenticationStrategy> = emptyList()
) {
    val hasSignInMethods: Boolean
        get() = local != null || identityProviders.isNotEmpty()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    preferencesRepository: PreferencesRepository
) : ViewModel() {
    val authenticationState: Flow<AuthenticationState> =
        preferencesRepository.authenticationStrategies
            .map { strategies ->
                val local = strategies.firstOrNull { it.type == AuthenticationStrategyType.LOCAL }
                val identityProviders = strategies.filter { it.type != AuthenticationStrategyType.LOCAL }
                AuthenticationState(local, identityProviders)
            }
            .flowOn(Dispatchers.IO)
}
