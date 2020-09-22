package mil.nga.giat.mage.login

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_account_created.*
import mil.nga.giat.mage.R

class AccountStateActivity: Activity() {
    companion object {
        private const val EXTRA_ACCOUNT_ACTIVE = "EXTRA_ACCOUNT_ACTIVE"
        private const val EXTRA_ACCOUNT_ENABLED = "EXTRA_ACCOUNT_ENABLED"

        fun intent(context: Context?, uri: Uri?): Intent {
            val intent = Intent(context, AccountStateActivity::class.java)
            intent.putExtra(EXTRA_ACCOUNT_ACTIVE, uri?.getQueryParameter("active") == "true")
            intent.putExtra(EXTRA_ACCOUNT_ENABLED, uri?.getQueryParameter("enabled") == "true")
            return intent
        }
    }

    override fun onCreate(savedInstanceBundle: Bundle?) {
        super.onCreate(savedInstanceBundle)

        setContentView(R.layout.activity_account_created)

        val appName = findViewById<TextView>(R.id.mage)
        appName.typeface = Typeface.createFromAsset(assets, "fonts/GondolaMage-Regular.otf")

        val accountActive = intent.getBooleanExtra(EXTRA_ACCOUNT_ACTIVE, false)
        val accountEnabled = intent.getBooleanExtra(EXTRA_ACCOUNT_ENABLED, false)

        if (!accountActive) {
            status.text = getString(R.string.account_inactive_title)
            message.text = getString(R.string.account_inactive_message)
        } else if (!accountEnabled) {
            status.text = getString(R.string.account_disabled_title)
            message.text = getString(R.string.account_disabled_message)
        }

        ok.setOnClickListener { finish() }
    }
}