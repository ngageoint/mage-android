package mil.nga.giat.mage.login

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.Spanned
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.util.Patterns
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_signup.*
import kotlinx.android.synthetic.main.mage_header.*
import mil.nga.giat.mage.R
import mil.nga.giat.mage.contact.utilities.LinkGenerator
import mil.nga.giat.mage.login.SignupViewModel.*
import javax.inject.Inject

@AndroidEntryPoint
open class SignupActivity : AppCompatActivity() {

   protected lateinit var viewModel: SignupViewModel
   @Inject
   protected lateinit var preferences: SharedPreferences

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

      viewModel = ViewModelProvider(this).get(SignupViewModel::class.java)
      viewModel.signupState.observe(this, { state: SignupState -> onSignupState(state) })
      viewModel.signupStatus.observe(this, { status: SignupStatus? -> onSignup(status) })
      viewModel.captcha.observe(this, { captcha: String? -> onCaptcha(captcha) })
      viewModel.captchaState.observe(this, { state: CaptchaState -> onCaptchaState(state) })

   }

   protected fun onSignupState(state: SignupState) {
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

   protected fun onSignup(status: SignupStatus?) {
      if (status == null) return

      if (status.success) {
         val isActive = status.user!!.get("active").asBoolean
         val username = status.user!!.get("username").asString
         showSignupSuccessDialog(isActive, username)
      } else {
         if (status.error == SignupError.INVALID_USERNAME) {
            captcha_text.setText("")
            username_layout.error = "Username not available"
         }

         val message = status.errorMessage

         val dialog = AlertDialog.Builder(this)
            .setTitle("Signup Failed")
            .setMessage(addLinks(message, "", ""))
            .setPositiveButton(android.R.string.ok, null)
            .show()

         (dialog.findViewById<View>(android.R.id.message) as TextView).movementMethod =
            LinkMovementMethod.getInstance()
      }
   }

   open protected fun signup() {
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

   protected fun hideKeyboard() {
      val view: View = findViewById(android.R.id.content)
      val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
      imm.hideSoftInputFromWindow(view.windowToken, 0)
   }

   private fun showSignupSuccessDialog(isActive: Boolean, username: String) {
      val alertDialog = AlertDialog.Builder(this)
      alertDialog.setTitle(R.string.account_inactive_title)
      if (isActive) {
         val message = getString(R.string.account_active_message);
         alertDialog.setMessage(addLinks(message, username, ""))
      } else {
         val message = getString(R.string.account_inactive_message);
         alertDialog.setMessage(addLinks(message, username, ""))
      }
      alertDialog.setPositiveButton("Ok") { _: DialogInterface?, _: Int -> done() }
      val dialog = alertDialog.show()
      (dialog.findViewById<View>(android.R.id.message) as TextView).movementMethod =
         LinkMovementMethod.getInstance()
   }

   private fun addLinks(
      message: String?,
      identifier: String,
      strategy: String
   ): Spanned? {
      val emailLink =
         LinkGenerator.getEmailLink(this.preferences, message, identifier, strategy)
      val phoneLink = LinkGenerator.getPhoneLink(this.preferences)
      return Html.fromHtml(
         message + " <br /><br /> "
                 + "You may contact your MAGE administrator via <a href= "
                 + emailLink + ">Email</a> or <a href="
                 + phoneLink + ">Phone</a> for further assistance."
      )
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