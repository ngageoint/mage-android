package mil.nga.giat.mage.disclaimer

import android.content.Intent
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mil.nga.giat.mage.event.EventsActivity
import mil.nga.giat.mage.login.LoginActivity
import mil.nga.giat.mage.ui.theme.MageTheme3

/**
 * Launches the disclaimer message after login
 */
@AndroidEntryPoint
class DisclaimerActivity : AppCompatActivity() {
   private val viewModel: DisclaimerViewModel by viewModels()

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      requestWindowFeature(Window.FEATURE_NO_TITLE)
      window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

      setContent {
         MageTheme3 {
            val disclaimer by viewModel.disclaimer.collectAsState()
            disclaimer?.let { current ->
               DisclaimerScreen(
                  title = current.title,
                  text = current.text,
                  onAgree = { viewModel.agree() },
                  onDisagree = { viewModel.disagree() }
               )
            }
         }
      }

      lifecycleScope.launch {
         repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
               when (event) {
                  DisclaimerEvent.NavigateToEvents -> agree()
                  DisclaimerEvent.NavigateToLogin -> disagree()
               }
            }
         }
      }
   }

   override fun onResume() {
      super.onResume()
      viewModel.checkDisclaimer()
   }

   private fun agree() {
      val intent = Intent(applicationContext, EventsActivity::class.java)
      getIntent().extras?.let { intent.putExtras(it) }
      startActivity(intent)
      finish()
   }

   private fun disagree() {
      startActivity(Intent(applicationContext, LoginActivity::class.java))
      finish()
   }
}
