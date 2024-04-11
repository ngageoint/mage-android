package mil.nga.giat.mage.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import mil.nga.giat.mage.ui.theme.MageTheme3
import mil.nga.giat.mage.ui.url.ServerUrlScreen

@AndroidEntryPoint
class ServerUrlActivity : AppCompatActivity() {
   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      setContent {
         MageTheme3 {
            ServerUrlScreen(
               onDone = {
                  val intent = Intent(this, LoginActivity::class.java)
                  startActivity(intent)
                  finish()
               },
               onCancel = {
                  val intent = Intent(this, LoginActivity::class.java)
                  startActivity(intent)
                  finish()
               }
            )
         }
      }
   }
}