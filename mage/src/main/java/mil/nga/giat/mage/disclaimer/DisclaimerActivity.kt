package mil.nga.giat.mage.disclaimer

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.preference.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import mil.nga.giat.mage.R
import mil.nga.giat.mage.di.SessionController
import mil.nga.giat.mage.event.EventsActivity
import mil.nga.giat.mage.login.LoginActivity
import mil.nga.giat.mage.ui.theme.MageTheme3
import javax.inject.Inject

private data class Disclaimer(val title: String, val text: String)

@AndroidEntryPoint
class DisclaimerActivity : AppCompatActivity() {
   @Inject lateinit var sessionController: SessionController
   @Inject lateinit var preferences: SharedPreferences

   private var disclaimer by mutableStateOf<Disclaimer?>(null)

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      requestWindowFeature(Window.FEATURE_NO_TITLE)
      window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

      setContent {
         MageTheme3 {
            disclaimer?.let { current ->
               DisclaimerScreen(
                  title = current.title,
                  text = current.text,
                  onAgree = { agree() },
                  onDisagree = { disagree() }
               )
            }
         }
      }
   }

   override fun onResume() {
      super.onResume()
      val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
      val showDisclaimer = sharedPreferences.getBoolean(getString(R.string.serverDisclaimerShow), false)
      if (showDisclaimer) {
         disclaimer = Disclaimer(
            title = sharedPreferences.getString(getString(R.string.serverDisclaimerTitle), null).orEmpty(),
            text = sharedPreferences.getString(getString(R.string.serverDisclaimerText), null).orEmpty()
         )
      } else {
         agree()
      }
   }

   private fun agree() {
      val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
      sharedPreferences.edit().putBoolean(getString(R.string.disclaimerAcceptedKey), true).apply()
      val intent = Intent(applicationContext, EventsActivity::class.java)
      getIntent().extras?.let { intent.putExtras(it) }
      startActivity(intent)
      finish()
   }

   private fun disagree() {
      sessionController.onLogout(true)
      startActivity(Intent(applicationContext, LoginActivity::class.java))
      finish()
   }
}
