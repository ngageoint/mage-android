package mil.nga.giat.mage.login

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import dagger.android.support.DaggerAppCompatActivity
import kotlinx.android.synthetic.main.activity_signup.*
import kotlinx.android.synthetic.main.mage_header.*
import mil.nga.giat.mage.R
import mil.nga.giat.mage.login.SignupViewModel.*
import javax.inject.Inject


class SignupActivity : DaggerAppCompatActivity() {

	@Inject
   lateinit var viewModelFactory: ViewModelProvider.Factory
   private lateinit var viewModel: SignupViewModel

   public override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      setContentView(R.layout.activity_signup)

      mage.typeface = Typeface.createFromAsset(assets, "fonts/GondolaMage-Regular.otf")

      signup_button.setOnClickListener { signup() }
      cancel_button.setOnClickListener { cancel() }
      refresh_captcha.setOnClickListener { getCaptcha() }

      signup_username.setOnFocusChangeListener { _: View, hasFocus: Boolean ->
         if (!hasFocus) {
            getCaptcha()
         }
      }

      signup_password.addTextChangedListener(object : TextWatcher {
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

      captcha_text.setOnKeyListener { _, keyCode, _ ->
         if (keyCode == KeyEvent.KEYCODE_ENTER) {
            signup()
            return@setOnKeyListener true
         } else {
            return@setOnKeyListener false
         }
      }

      viewModel = ViewModelProvider(this, viewModelFactory).get(SignupViewModel::class.java)
      viewModel.signupState.observe(this, Observer { state: SignupState -> onSignupState(state) })
      viewModel.signupStatus.observe(this, Observer { status: SignupStatus? -> onSignup(status) })
      viewModel.captcha.observe(this, Observer { captcha: String? -> onCaptcha(captcha) })
      viewModel.captchaState.observe(this, Observer { state: CaptchaState -> onCaptchaState(state) })

   }

   private fun onSignupState(state: SignupState) {
      if (state == SignupState.CANCEL) {
         done()
      } else {
         toggleMask(state === SignupState.LOADING)
      }
   }

   private fun onCaptchaState(state: CaptchaState) {
      captcha_progress.visibility = if (state == CaptchaState.LOADING) View.VISIBLE else View.GONE
   }

   private fun onCaptcha(captcha: String?) {
      if (captcha != null) {
         web_view.visibility = View.VISIBLE
         refresh_captcha.visibility = View.VISIBLE
         val html = "<html><body style=\"background-color: ${backgroundColor()};\"><div style=\"width: 100%; height:100%;\" ><img src=\"$captcha\" width=\"100%\" height=\"100%\"\"/></div></body></html>"
         web_view.loadDataWithBaseURL(null, html, "text/html; charset=utf-8", "utf8", null)
      } else {
         web_view.visibility = View.GONE
         refresh_captcha.visibility = View.VISIBLE
      }
   }

   private fun onSignup(status: SignupStatus?) {
      if (status == null) return

      if (status.success) {
         val isActive = status.user!!.get("active").asBoolean
         showSignupSuccessDialog(isActive)
      } else {
         if (status.error == SignupError.INVALID_USERNAME) {
            captcha_text.setText("")
            username_layout.error = "Username not available"
         }

         AlertDialog.Builder(this)
            .setTitle("Signup Failed")
            .setMessage(status.errorMessage)
            .setPositiveButton(android.R.string.ok, null)
            .show()
      }
   }

   private fun signup() {
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

      val captchaText = captcha_text.text.toString()
      if (captchaText.isEmpty()) {
         captcha_text_layout.error = "Captcha text cannot be blank"
         return
      }

      hideKeyboard()

      viewModel.signup(Account(username, displayName, email, phone, password), captchaText)
   }

   private fun getCaptcha() {
      viewModel.getCaptcha(signup_username.text.toString(), backgroundColor())
   }

   private fun cancel() {
      viewModel.cancel()
   }

   private fun hideKeyboard() {
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
      mask.visibility = if (visible) View.VISIBLE else View.GONE
      progress.visibility = if (visible) View.VISIBLE else View.GONE
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