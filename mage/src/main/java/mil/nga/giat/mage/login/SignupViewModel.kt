package mil.nga.giat.mage.login

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import mil.nga.giat.mage.data.repository.user.UserRepository
import javax.inject.Inject

@HiltViewModel
open class SignupViewModel @Inject constructor(
   val preferences: SharedPreferences,
   private val userRepository: UserRepository
): ViewModel() {

   enum class CaptchaState {
      COMPLETE, LOADING
   }

   enum class SignupState {
      COMPLETE, LOADING
   }

   enum class SignupError {
      INVALID_CAPTCHA, INVALID_USERNAME
   }

   private var username = ""
   private var backgroundColor = "#FFFFFF"

   data class Account(val username: String, val displayName: String, val email: String, val phone: String, val password: String)
   var account: Account? =  null

   data class SignupStatus(val success: Boolean, val user: JsonObject?, val error: SignupError? = null, val errorMessage: String? = null, val username: String? = null)

   private val _captchaState = MutableLiveData<CaptchaState>()
   val captchaState: LiveData<CaptchaState> = _captchaState

   protected val _signupState = MutableLiveData<SignupState>()
   val signupState: LiveData<SignupState> = _signupState

   protected val _signupStatus = MutableLiveData<SignupStatus?>()
   val signupStatus: LiveData<SignupStatus?> = _signupStatus

   private val _captcha = MutableLiveData<String?>()
   val captcha: LiveData<String?> = _captcha

   private var captchaToken: String? = ""

   fun getCaptcha(username: String, backgroundColor: String) {
      this.username = username
      this.backgroundColor = backgroundColor
      _captchaState.value = CaptchaState.LOADING

      viewModelScope.launch {
         try {
            val response = userRepository.getCaptcha(username, backgroundColor)
            if (response.isSuccessful) {
               val json = response.body()!!
               captchaToken = json.get("token").asString
               _captcha.value = json.get("captcha").asString
            }

            _captchaState.value = CaptchaState.COMPLETE
         } catch (e: Exception) {
            _captchaState.value = CaptchaState.COMPLETE
         }
      }
   }

   open fun signup(account: Account, captchaText: String) {
      viewModelScope.launch {
         _signupStatus.value = null
         _signupState.value = SignupState.LOADING

         try {
            val response = userRepository.verifyUser(account.displayName, account.email, account.phone, account.password, captchaText, captchaToken)
            if (response.isSuccessful) {
               _signupStatus.value = SignupStatus(true, response.body())
            } else {
               if (response.code() == 401) {
                  getCaptcha(username, backgroundColor)
               } else if (response.code() == 409) {
                  _captcha.value = null
               }

               val error = if (response.code() == 409) SignupError.INVALID_USERNAME else SignupError.INVALID_CAPTCHA
               _signupStatus.value = SignupStatus(false, null, error, response.errorBody()?.string(), account.username)
            }
         } catch (e: Exception) {
            _signupStatus.value = SignupStatus(false, null, null, e.localizedMessage, account.username)
         }

         _signupState.value = SignupState.COMPLETE
      }
   }
}
