package mil.nga.giat.mage.profile

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.JsonObject
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mil.nga.giat.mage.MageApplication
import mil.nga.giat.mage.R
import mil.nga.giat.mage.data.datasource.user.UserLocalDataSource
import mil.nga.giat.mage.data.repository.user.UserRepository
import mil.nga.giat.mage.login.LoginActivity
import mil.nga.giat.mage.login.PasswordStrengthFragment
import mil.nga.giat.mage.sdk.exceptions.UserException
import okhttp3.ResponseBody
import org.apache.commons.lang3.StringUtils
import retrofit2.Response
import javax.inject.Inject

@AndroidEntryPoint
class ChangePasswordActivity : AppCompatActivity() {
   @Inject lateinit var application: MageApplication
   @Inject lateinit var userRepository: UserRepository
   @Inject lateinit var userLocalDataSource: UserLocalDataSource

   private var username: String? = null
   private lateinit var password: TextInputEditText
   private lateinit var passwordLayout: TextInputLayout
   private lateinit var newPassword: TextInputEditText
   private lateinit var newPasswordLayout: TextInputLayout
   private lateinit var newPasswordConfirm: TextInputEditText
   private lateinit var newPasswordConfirmLayout: TextInputLayout

   public override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setContentView(R.layout.activity_change_password)

      val toolbar = findViewById<Toolbar>(R.id.change_pwd_toolbar)
      ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v: View, windowInsets: WindowInsetsCompat ->
         val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
         v.setPadding(insets.left, 0, insets.right, 0)
         windowInsets
      }

      setSupportActionBar(toolbar)

      supportActionBar?.let {
         it.setDisplayHomeAsUpEnabled(true)
         it.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24dp)
      }

      val scrollView = findViewById<View>(R.id.change_pwd_scrollview)
      ViewCompat.setOnApplyWindowInsetsListener(scrollView) { v: View, windowInsets: WindowInsetsCompat ->
         val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
         v.setPadding(insets.left, 0, insets.right, insets.bottom)
         windowInsets
      }

      val passwordStrengthFragment =
         supportFragmentManager.findFragmentById(R.id.password_strength_fragment) as PasswordStrengthFragment?

      try {
         userLocalDataSource.readCurrentUser()?.let { user ->
            username = user.username
            val sanitizedPasswordInputs: MutableList<String> = ArrayList()
            sanitizedPasswordInputs.add(user.username)
            sanitizedPasswordInputs.add(user.displayName)
            sanitizedPasswordInputs.add(user.email)
            sanitizedPasswordInputs.removeAll(setOf<Any?>(null))
            passwordStrengthFragment!!.setSanitizedList(sanitizedPasswordInputs)
         }
      } catch (e: UserException) {
         Log.e(LOG_NAME, "Problem finding current user.", e)
      }

      password = findViewById<View>(R.id.password) as TextInputEditText
      passwordLayout = findViewById<View>(R.id.password_layout) as TextInputLayout
      newPassword = findViewById<View>(R.id.new_password) as TextInputEditText
      newPasswordLayout = findViewById<View>(R.id.new_password_layout) as TextInputLayout
      newPasswordConfirm = findViewById<View>(R.id.new_password_confirm) as TextInputEditText
      newPasswordConfirmLayout = findViewById<View>(R.id.new_password_confirm_layout) as TextInputLayout
      newPassword = findViewById<View>(R.id.new_password) as TextInputEditText
      newPassword.addTextChangedListener(object : TextWatcher {
         override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
         override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
         override fun afterTextChanged(s: Editable) {
            passwordStrengthFragment!!.onPasswordChanged(s.toString())
         }
      })
   }

   override fun onOptionsItemSelected(item: MenuItem): Boolean {
      when (item.itemId) {
         android.R.id.home -> {
            finish()
            return true
         }
      }
      return true
   }

   fun onChangePasswordClick(v: View?) {
      if (!validateInputs()) {
         return
      }

      CoroutineScope(Dispatchers.IO).launch {
         try {
            val response = userRepository.changePassword(
               username,
               password.text.toString(),
               newPassword.text.toString(),
               newPasswordConfirm.text.toString())

            withContext(Dispatchers.Main) {
               if (response.isSuccessful) {
                  onSuccess()
               } else {
                  onError(response)
               }
            }
         } catch (e: Exception) {
            withContext(Dispatchers.Main) {
               onError(null)
            }
         }
      }
   }

   private fun onSuccess() {
      val dialog = AlertDialog.Builder(this)
         .setTitle("Password Changed")
         .setMessage("Your password has been changed, for security purposes you will need to login with your new password.")
         .setCancelable(false)
         .setPositiveButton(android.R.string.ok) { dialog, which ->
            application.onLogout(true)

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

            finish()
         }.create()
      dialog.show()
   }

   private fun onError(response: Response<ResponseBody>?) {
      if (response == null) {
         val dialog = AlertDialog.Builder(this)
            .setTitle("No connection")
            .setMessage("Please ensure you have an internet connection and try again")
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok, null).create()
         dialog.show()
      } else {
         val errorCode = response.code()
         if (errorCode == 401) {
            passwordLayout.error = "Invalid password, please check your password and try again"
         } else {
            val dialog = AlertDialog.Builder(this)
               .setTitle("Error changing password")
               .setMessage(response.message())
               .setCancelable(false)
               .setPositiveButton(android.R.string.ok, null).create()
            dialog.show()
         }
      }
   }

   private fun validateInputs(): Boolean {
      passwordLayout.error = null
      newPasswordLayout.error = null
      newPasswordConfirmLayout.error = null
      if (StringUtils.isBlank(password.text.toString())) {
         passwordLayout.error = "Password is required"
         return false
      }
      if (StringUtils.isBlank(newPassword.text.toString())) {
         newPasswordLayout.error = "New Password is required"
         return false
      }
      if (newPassword.text.toString() != newPasswordConfirm.text.toString()) {
         newPasswordLayout.error = "Passwords do not match"
         newPasswordConfirmLayout.error = "Passwords do not match"
         return false
      }
      if (password.text.toString() == newPassword.text.toString()) {
         newPasswordLayout.error = "New password cannot be the same as current password."
         newPasswordConfirmLayout.error = "New password cannot be the same as current password."
         return false
      }
      return true
   }

   companion object {
      private val LOG_NAME = ChangePasswordActivity::class.java.name
   }
}