package mil.nga.giat.mage.compat.server5.login

import android.content.SharedPreferences
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import mil.nga.giat.mage.data.repository.user.UserRepository
import mil.nga.giat.mage.login.SignupViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class SignupViewModelServer5 @Inject constructor(
   preferences: SharedPreferences,
   private val userResource: UserResourceServer5,
   private val userRepository: UserRepository
): SignupViewModel(preferences, userRepository) {

   fun signup(account: Account) {
      userResource.create(account.username, account.displayName, account.email, account.phone, account.password, object: Callback<JsonObject> {
         override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
            if (response.isSuccessful) {
               _signupStatus.value = SignupStatus(true, response.body())
            } else {
               _signupStatus.value = SignupStatus(false, null, null, response.errorBody()?.string())
            }

            _signupState.value = SignupState.COMPLETE
         }

         override fun onFailure(call: Call<JsonObject>, t: Throwable) {
            _signupStatus.value = SignupStatus(false, null, null, t.localizedMessage)
         }
      })

      _signupStatus.value = null
      _signupState.value = SignupState.LOADING
   }
}