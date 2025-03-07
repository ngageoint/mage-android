package mil.nga.giat.mage.login

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import mil.nga.giat.mage.ui.setup.AccountState
import mil.nga.giat.mage.ui.setup.AccountStateScreen
import mil.nga.giat.mage.ui.theme.MageTheme3

@AndroidEntryPoint
class AccountStateActivity: AppCompatActivity() {
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

        val accountActive = intent.getBooleanExtra(EXTRA_ACCOUNT_ACTIVE, false)
        val accountEnabled = intent.getBooleanExtra(EXTRA_ACCOUNT_ENABLED, false)

        val accountState = if (!accountActive) {
            AccountState.Inactive(applicationContext)
        } else if (!accountEnabled) {
            AccountState.Disabled(applicationContext)
        } else AccountState.Unknown(applicationContext)

        setContent {
            MageTheme3 {
                AccountStateScreen(
                    accountState = accountState,
                    onDone = { finish() }
                )
            }
        }
    }
}