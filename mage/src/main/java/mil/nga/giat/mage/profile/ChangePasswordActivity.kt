package mil.nga.giat.mage.profile

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import mil.nga.giat.mage.MageApplication
import mil.nga.giat.mage.login.LoginActivity
import javax.inject.Inject

@AndroidEntryPoint
class ChangePasswordActivity : AppCompatActivity() {
   @Inject lateinit var application: MageApplication

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      setContent {
         ChangePasswordScreen(
            onNavigateUp = { finish() },
            onPasswordChanged = { onPasswordChanged() }
         )
      }
   }

   private fun onPasswordChanged() {
      application.onLogout(true)
      val intent = Intent(this, LoginActivity::class.java)
      intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
      startActivity(intent)
      finish()
   }
}
