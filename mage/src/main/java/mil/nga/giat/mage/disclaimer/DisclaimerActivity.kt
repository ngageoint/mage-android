package mil.nga.giat.mage.disclaimer

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import mil.nga.giat.mage.MageApplication
import mil.nga.giat.mage.R
import mil.nga.giat.mage.event.EventsActivity
import mil.nga.giat.mage.login.LoginActivity
import javax.inject.Inject

@AndroidEntryPoint
class DisclaimerActivity : AppCompatActivity() {
	@Inject lateinit var application: MageApplication
   @Inject lateinit var preferences: SharedPreferences

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      requestWindowFeature(Window.FEATURE_NO_TITLE)
      setContentView(R.layout.activity_disclaimer)
      window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
   }

   override fun onResume() {
      super.onResume()
      val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
      val showDisclaimer = sharedPreferences.getBoolean(getString(R.string.serverDisclaimerShow), false)
      if (showDisclaimer) {
         val disclaimerTitle = sharedPreferences.getString(getString(R.string.serverDisclaimerTitle), null)
         val disclaimerTitleView = findViewById<TextView>(R.id.disclaimer_title)
         disclaimerTitleView.text = disclaimerTitle
         val disclaimerTextView = findViewById<TextView>(R.id.disclaimer_text)
         val disclaimerText = sharedPreferences.getString(getString(R.string.serverDisclaimerText), null)
         disclaimerTextView.text = disclaimerText
      } else {
         agree(null)
      }
   }

   fun agree(view: View?) {
      val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(
         applicationContext
      )
      sharedPreferences.edit().putBoolean(getString(R.string.disclaimerAcceptedKey), true).apply()
      val intent = Intent(applicationContext, EventsActivity::class.java)
      val extras = getIntent().extras
      if (extras != null) {
         intent.putExtras(extras)
      }
      startActivity(intent)
      finish()
   }

   fun exit(view: View?) {
      application.onLogout(true)
      startActivity(Intent(applicationContext, LoginActivity::class.java))
      finish()
   }

   override fun onBackPressed() {}
}