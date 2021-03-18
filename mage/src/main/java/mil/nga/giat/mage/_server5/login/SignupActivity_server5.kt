package mil.nga.giat.mage._server5.login

import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.activity_signup.*
import mil.nga.giat.mage.login.SignupActivity
import mil.nga.giat.mage.login.SignupViewModel

class SignupActivity_server5: SignupActivity() {

   protected lateinit var viewModel_server5: SignupViewModel_server5

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      captcha_view.visibility = View.GONE
      captcha_text_layout.visibility = View.GONE

      viewModel_server5 = ViewModelProvider(this, viewModelFactory).get(SignupViewModel_server5::class.java)
      viewModel_server5.signupState.observe(this, Observer { state: SignupViewModel.SignupState -> onSignupState(state) })
      viewModel_server5.signupStatus.observe(this, Observer { status: SignupViewModel.SignupStatus? -> onSignup(status) })
   }

   override fun signup() {
      displayname_layout.error = null
      username_layout.error = null
      email_layout.error = null
      password_layout.error = null
      confirmpassword_layout.error = null

      val displayName: String = signup_displayname.text.toString()
      val username: String = signup_username.text.toString()
      val email: String = signup_email.text.toString()
      val phone: String = signup_phone.text.toString()

      // are the inputs valid?
      if (displayName.isEmpty()) {
         displayname_layout.error = "Display name can not be blank"
         return
      }
      if (username.isEmpty()) {
         username_layout.error = "Username can not be blank"
         return
      }

      if (email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
         email_layout.error = "Please enter a valid email address"
         return
      }

      val password: String = signup_password.text.toString()
      val confirmpassword: String = signup_confirmpassword.text.toString()
      if (password.isEmpty()) {
         password_layout.error = "Password can not be blank"
         return
      }

      if (confirmpassword.isEmpty()) {
         confirmpassword_layout.error = "Enter password again"
         return
      }

      if (password != confirmpassword) {
         password_layout.error = "Passwords do not match"
         confirmpassword_layout.error = "Passwords do not match"
         return
      }

      hideKeyboard()

      viewModel_server5.signup(SignupViewModel.Account(username, displayName, email, phone, password))
   }


}