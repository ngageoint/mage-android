package mil.nga.giat.mage.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import mil.nga.giat.mage.ui.theme.MageTheme3
import mil.nga.giat.mage.ui.setup.TopLevelServerUrlScreen
import mil.nga.giat.mage.utils.IntentConstants
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import mil.nga.giat.mage.R

@AndroidEntryPoint
class ServerUrlActivity : AppCompatActivity() {
   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      val launchedFromLogin = intent.getBooleanExtra(IntentConstants.LAUNCHED_FROM_BUTTON_CLICK, false)

      val onDone = {
         //reset disclaimer acceptance on server change
         PreferenceManager.getDefaultSharedPreferences(application).edit() {
            putBoolean(getString(R.string.disclaimerAcceptedKey), false)
         }

         val intent = Intent(this, LoginActivity::class.java)
         startActivity(intent)
         finish()
      }

      setContent {
         TopLevelServerUrlScreen(onDone)

         if (launchedFromLogin) {
            BackHandler { onDone() }
         }
      }
   }
}