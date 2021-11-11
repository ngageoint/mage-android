package mil.nga.giat.mage.login

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import mil.nga.giat.mage.sdk.http.resource.UserResource
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
open class SignupViewModel @Inject constructor(
   @ApplicationContext val context: Context,
   val preferences: SharedPreferences
): ViewModel() {

   enum class CaptchaState {
      COMPLETE, LOADING
   }

   enum class SignupState {
      COMPLETE, LOADING, CANCEL
   }

   enum class SignupError {
      INVALID_CAPTCHA, INVALID_USERNAME
   }

   private val userResource = UserResource(context)
   private var username = ""
   private var backgroundColor = "#FFFFFF"

   data class Account(val username: String, val displayName: String, val email: String, val phone: String, val password: String)
   var account: Account? =  null

   data class SignupStatus(val success: Boolean, val user: JsonObject?, val error: SignupError? = null, val errorMessage: String? = null, val username: String? = null)

   private val _captchaState = MutableLiveData<CaptchaState>()
   val captchaState: LiveData<CaptchaState> = _captchaState

   protected val _signupState = MutableLiveData<SignupState>()
   val signupState: LiveData<SignupState> = _signupState

   protected val _signupStatus = MutableLiveData<SignupStatus>()
   val signupStatus: LiveData<SignupStatus> = _signupStatus

   private val _captcha = MutableLiveData<String>()
   val captcha: LiveData<String> = _captcha

   private var captchaToken: String? = ""

   fun getCaptcha(username: String, backgroundColor: String) {
      this.username = username
      this.backgroundColor = backgroundColor

      userResource.getCaptcha(username, backgroundColor, object: Callback<JsonObject> {
         override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
            if (response.isSuccessful) {
               val json = response.body()!!
               captchaToken = json.get("token").asString
               _captcha.value = json.get("captcha").asString
            }

            _captchaState.value = CaptchaState.COMPLETE
         }

         override fun onFailure(call: Call<JsonObject>, t: Throwable) {
            _captchaState.value = CaptchaState.COMPLETE
         }
      })

      _captchaState.value = CaptchaState.LOADING
   }

   open fun signup(account: Account, captchaText: String) {
      val userResource = UserResource(context)
      userResource.verifyUser(account.displayName, account.email, account.phone, account.password, captchaText, captchaToken, object: Callback<JsonObject> {
         override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
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

            _signupState.value = SignupState.COMPLETE
         }

         override fun onFailure(call: Call<JsonObject>, t: Throwable) {
            _signupStatus.value = SignupStatus(false, null, null, t.localizedMessage, account.username)
         }
      })

      _signupStatus.value = null
      _signupState.value = SignupState.LOADING
   }

   fun cancel() {
      _signupState.value = SignupState.CANCEL
   }
}
