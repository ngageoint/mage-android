package mil.nga.giat.mage.compat.server5.login

import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint
import mil.nga.giat.mage.login.SignupActivity
import mil.nga.giat.mage.login.SignupViewModel

@AndroidEntryPoint
class SignupActivityServer5: SignupActivity() {

   protected lateinit var viewModelServer5: SignupViewModelServer5

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      binding.captchaView.visibility = View.GONE
      binding.captchaTextLayout.visibility = View.GONE

      viewModelServer5 = ViewModelProvider(this).get(SignupViewModelServer5::class.java)
      viewModelServer5.signupState.observe(this, { state: SignupViewModel.SignupState -> onSignupState(state) })
      viewModelServer5.signupStatus.observe(this, { status: SignupViewModel.SignupStatus? -> onSignup(status) })
   }

   override fun signup() {
      binding.displaynameLayout.error = null
      binding.usernameLayout.error = null
      binding.emailLayout.error = null
      binding.passwordLayout.error = null
      binding.confirmpasswordLayout.error = null

      val displayName: String = binding.signupDisplayname.text.toString()
      val username: String = binding.signupUsername.text.toString()
      val email: String = binding.signupEmail.text.toString()
      val phone: String = binding.signupPhone.text.toString()

      // are the inputs valid?
      if (displayName.isEmpty()) {
         binding.displaynameLayout.error = "Display name can not be blank"
         return
      }
      if (username.isEmpty()) {
         binding.usernameLayout.error = "Username can not be blank"
         return
      }

      if (email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
         binding.emailLayout.error = "Please enter a valid email address"
         return
      }

      val password: String = binding.signupPassword.text.toString()
      val confirmpassword: String = binding.signupConfirmpassword.text.toString()
      if (password.isEmpty()) {
         binding.passwordLayout.error = "Password can not be blank"
         return
      }

      if (confirmpassword.isEmpty()) {
         binding.confirmpasswordLayout.error = "Enter password again"
         return
      }

      if (password != confirmpassword) {
         binding.passwordLayout.error = "Passwords do not match"
         binding.confirmpasswordLayout.error = "Passwords do not match"
         return
      }

      hideKeyboard()

      viewModelServer5.signup(SignupViewModel.Account(username, displayName, email, phone, password))
   }
}