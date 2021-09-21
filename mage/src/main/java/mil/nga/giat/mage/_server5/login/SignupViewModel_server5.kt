package mil.nga.giat.mage._server5.login

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import mil.nga.giat.mage.login.SignupViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class SignupViewModel_server5 @Inject constructor(
   @ApplicationContext context: Context,
   preferences: SharedPreferences
): SignupViewModel(context, preferences) {

   fun signup(account: Account) {
      val userResource = UserResource_server5(context)
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