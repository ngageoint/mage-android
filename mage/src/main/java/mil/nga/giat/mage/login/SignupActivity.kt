package mil.nga.giat.mage.login

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint
import mil.nga.giat.mage.R
import mil.nga.giat.mage.databinding.ActivitySignupBinding
import mil.nga.giat.mage.login.SignupViewModel.Account
import mil.nga.giat.mage.login.SignupViewModel.CaptchaState
import mil.nga.giat.mage.login.SignupViewModel.SignupError
import mil.nga.giat.mage.login.SignupViewModel.SignupState
import mil.nga.giat.mage.login.SignupViewModel.SignupStatus

@AndroidEntryPoint
open class SignupActivity : AppCompatActivity() {

   protected lateinit var binding: ActivitySignupBinding
   protected lateinit var viewModel: SignupViewModel

   public override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      binding = ActivitySignupBinding.inflate(layoutInflater)
      setContentView(binding.root)

      binding.signupButton.setOnClickListener { signup() }
      binding.cancelButton.setOnClickListener { cancel() }
      binding.refreshCaptcha.setOnClickListener { getCaptcha() }

      binding.signupUsername.setOnFocusChangeListener { _: View, hasFocus: Boolean ->
         if (!hasFocus) {
            getCaptcha()
         }
      }

      binding.signupPassword.addTextChangedListener(object : TextWatcher {
         override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
         override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
         override fun afterTextChanged(s: Editable) {
            val sanitizedPasswordInputs: MutableList<String> = ArrayList()
            viewModel.account?.let {
               sanitizedPasswordInputs.add(it.username)
               sanitizedPasswordInputs.add(it.displayName)
               sanitizedPasswordInputs.add(it.email)
               sanitizedPasswordInputs.removeAll(setOf<Any?>(null))
            }

            val passwordStrengthFragment = supportFragmentManager.findFragmentById(R.id.password_strength_fragment) as PasswordStrengthFragment
            passwordStrengthFragment.setSanitizedList(sanitizedPasswordInputs)
            passwordStrengthFragment.onPasswordChanged(s.toString())
         }
      })

      binding.captchaText.setOnKeyListener { _, keyCode, _ ->
         if (keyCode == KeyEvent.KEYCODE_ENTER) {
            signup()
            return@setOnKeyListener true
         } else {
            return@setOnKeyListener false
         }
      }

      viewModel = ViewModelProvider(this).get(SignupViewModel::class.java)
      viewModel.signupState.observe(this) { state: SignupState -> onSignupState(state) }
      viewModel.signupStatus.observe(this) { status: SignupStatus? -> onSignup(status) }
      viewModel.captcha.observe(this) { captcha: String? -> onCaptcha(captcha) }
      viewModel.captchaState.observe(this) { state: CaptchaState -> onCaptchaState(state) }
   }

   protected fun onSignupState(state: SignupState) {
      if (state == SignupState.CANCEL) {
         done()
      } else {
         toggleMask(state === SignupState.LOADING)
      }
   }

   private fun onCaptchaState(state: CaptchaState) {
      binding.captchaProgress.visibility = if (state == CaptchaState.LOADING) View.VISIBLE else View.GONE
   }

   private fun onCaptcha(captcha: String?) {
      if (captcha != null) {
         binding.webView.visibility = View.VISIBLE
         binding.refreshCaptcha.visibility = View.VISIBLE
         val html = "<html><body style=\"background-color: ${backgroundColor()};\"><div style=\"width: 100%; height:100%;\" ><img src=\"$captcha\" width=\"100%\" height=\"100%\"\"/></div></body></html>"
         binding.webView.loadDataWithBaseURL(null, html, "text/html; charset=utf-8", "utf8", null)
      } else {
         binding.webView.visibility = View.GONE
         binding.refreshCaptcha.visibility = View.VISIBLE
      }
   }

   protected fun onSignup(status: SignupStatus?) {
      if (status == null) return

      if (status.success) {
         val isActive = status.user!!.get("active").asBoolean
         showSignupSuccessDialog(isActive)
      } else {
         when (status.error) {
            SignupError.INVALID_USERNAME -> {
               binding.captchaText.setText("")
               binding.usernameLayout.error = "Username not available"
            }
            SignupError.INVALID_CAPTCHA -> {
               binding.captchaTextLayout.error = "Invalid Captcha"
            }
            else -> {
               AlertDialog.Builder(this)
                  .setTitle("Signup Failed")
                  .setMessage(status.errorMessage)
                  .setPositiveButton(android.R.string.ok, null)
                  .show()
            }
         }
      }
   }

   protected open fun signup() {
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

      val captchaText = binding.captchaText.text.toString()
      if (captchaText.isEmpty()) {
         binding.captchaTextLayout.error = "Captcha text cannot be blank"
         return
      }

      hideKeyboard()

      viewModel.signup(Account(username, displayName, email, phone, password), captchaText)
   }

   private fun getCaptcha() {
      viewModel.getCaptcha(binding.signupUsername.text.toString(), backgroundColor())
   }

   private fun cancel() {
      viewModel.cancel()
   }

   protected fun hideKeyboard() {
      val view: View = findViewById(android.R.id.content)
      val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
      imm.hideSoftInputFromWindow(view.windowToken, 0)
   }

   private fun showSignupSuccessDialog(isActive: Boolean) {
      val alertDialog = AlertDialog.Builder(this)
      alertDialog.setTitle(R.string.account_inactive_title)
      if (isActive) {
         alertDialog.setMessage(getString(R.string.account_active_message))
      } else {
         alertDialog.setMessage(getString(R.string.account_inactive_message))
      }
      alertDialog.setPositiveButton("Ok") { _: DialogInterface?, _: Int -> done() }
      alertDialog.show()
   }

   private fun toggleMask(visible: Boolean) {
      binding.progress.visibility = if (visible) View.VISIBLE else View.GONE
   }

   private fun backgroundColor(): String {
      val color = ColorUtils.compositeColors(ContextCompat.getColor(this, R.color.background), Color.WHITE)
      return String.format("#%06X", 0xFFFFFF and color)
   }

   private fun done() {
      startActivity(Intent(applicationContext, LoginActivity::class.java))
      finish()
   }
}