package mil.nga.giat.mage.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nulabinc.zxcvbn.Zxcvbn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mil.nga.giat.mage.data.datasource.user.UserLocalDataSource
import mil.nga.giat.mage.data.repository.user.UserRepository
import mil.nga.giat.mage.sdk.exceptions.UserException
import javax.inject.Inject

enum class PasswordStrength(val label: String) {
   WEAK("Weak"),
   FAIR("Fair"),
   GOOD("Good"),
   STRONG("Strong"),
   EXCELLENT("Excellent");

   companion object {
      fun fromScore(score: Int): PasswordStrength? {
         return entries.getOrNull(score)
      }
   }
}

sealed class ChangePasswordResult {
   data object Success : ChangePasswordResult()
   data object InvalidPassword : ChangePasswordResult()
   data class Error(val message: String?) : ChangePasswordResult()
   data object NoConnection : ChangePasswordResult()
}

data class ChangePasswordUiState(
   val password: String = "",
   val newPassword: String = "",
   val newPasswordConfirm: String = "",
   val passwordError: String? = null,
   val newPasswordError: String? = null,
   val newPasswordConfirmError: String? = null,
   val passwordStrength: PasswordStrength? = null,
   val isSubmitting: Boolean = false,
   val result: ChangePasswordResult? = null
)

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
   private val userRepository: UserRepository,
   private val userLocalDataSource: UserLocalDataSource
) : ViewModel() {

   private val zxcvbn = Zxcvbn()
   private var username: String? = null
   private var sanitizedInputs: List<String> = emptyList()

   private val _uiState = MutableStateFlow(ChangePasswordUiState())
   val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

   init {
      try {
         userLocalDataSource.readCurrentUser()?.let { user ->
            username = user.username
            sanitizedInputs = listOfNotNull(user.username, user.displayName, user.email)
         }
      } catch (_: UserException) {
         // No current user to seed the password-strength dictionary with
      }
   }

   fun onPasswordChanged(value: String) {
      _uiState.update { it.copy(password = value, passwordError = null) }
   }

   fun onNewPasswordChanged(value: String) {
      val score = zxcvbn.measure(value, sanitizedInputs).score
      _uiState.update {
         it.copy(
            newPassword = value,
            newPasswordError = null,
            passwordStrength = if (value.isEmpty()) null else PasswordStrength.fromScore(score)
         )
      }
   }

   fun onNewPasswordConfirmChanged(value: String) {
      _uiState.update { it.copy(newPasswordConfirm = value, newPasswordConfirmError = null) }
   }

   fun onResultHandled() {
      _uiState.update { it.copy(result = null) }
   }

   fun changePassword() {
      val state = _uiState.value

      if (state.password.isBlank()) {
         _uiState.update { it.copy(passwordError = "Password is required") }
         return
      }
      if (state.newPassword.isBlank()) {
         _uiState.update { it.copy(newPasswordError = "New Password is required") }
         return
      }
      if (state.newPassword != state.newPasswordConfirm) {
         _uiState.update {
            it.copy(
               newPasswordError = "Passwords do not match",
               newPasswordConfirmError = "Passwords do not match"
            )
         }
         return
      }
      if (state.password == state.newPassword) {
         _uiState.update {
            it.copy(
               newPasswordError = "New password cannot be the same as current password.",
               newPasswordConfirmError = "New password cannot be the same as current password."
            )
         }
         return
      }

      _uiState.update { it.copy(isSubmitting = true) }

      viewModelScope.launch {
         try {
            val response = userRepository.changePassword(
               username,
               state.password,
               state.newPassword,
               state.newPasswordConfirm
            )

            val result = if (response.isSuccessful) {
               ChangePasswordResult.Success
            } else if (response.code() == 401) {
               ChangePasswordResult.InvalidPassword
            } else {
               ChangePasswordResult.Error(response.message())
            }

            _uiState.update {
               it.copy(
                  isSubmitting = false,
                  result = result,
                  passwordError = if (result == ChangePasswordResult.InvalidPassword) {
                     "Invalid password, please check your password and try again"
                  } else {
                     it.passwordError
                  }
               )
            }
         } catch (_: Exception) {
            _uiState.update { it.copy(isSubmitting = false, result = ChangePasswordResult.NoConnection) }
         }
      }
   }
}
