package mil.nga.giat.mage.login.idp

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import androidx.appcompat.content.res.AppCompatResources
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.FragmentActivity
import mil.nga.giat.mage.R
import mil.nga.giat.mage.login.AccountStateActivity

/**
 * Created by wnewman on 10/14/15.
 */
class IdpLoginActivity : FragmentActivity() {
    companion object {
        private const val KEY_AUTHORIZATION_STARTED = "AUTHORIZATION_STARTED"
        private const val EXTRA_SERVER_URL = "EXTRA_SERVER_URL"
        private const val EXTRA_IDP_STRATEGY = "EXTRA_IDP_STRATEGY"

        const val EXTRA_IDP_TOKEN = "EXTRA_IDP_TOKEN"

        fun intent(context: Context?, url: String, strategy: String?): Intent {
            val intent = Intent(context, IdpLoginActivity::class.java)
            intent.putExtra(EXTRA_SERVER_URL, url)
            intent.putExtra(EXTRA_IDP_STRATEGY, strategy)
            return intent
        }
    }

    private var authorizationStarted = false
    private var serverURL: String? = null
    private var idpURL: String? = null
    private var  idpStrategy: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_idp)

        serverURL = intent.getStringExtra(EXTRA_SERVER_URL)
        idpStrategy = intent.getStringExtra(EXTRA_IDP_STRATEGY)
        idpURL = String.format("%s/auth/%s/signin?state=mobile", serverURL, idpStrategy)

        if (savedInstanceState == null) {
            extractState(intent.extras)
        } else {
            extractState(savedInstanceState)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onResume() {
        super.onResume()

        if (!authorizationStarted) {
            val customTabsIntent = CustomTabsIntent.Builder()
                    .setCloseButtonIcon(backButtonIcon())
                    .setToolbarColor(primaryColor())
                    .build()

            customTabsIntent.launchUrl(this, Uri.parse(idpURL))

            authorizationStarted = true

            return
        }

        if (intent.data != null) {
            handleAuthenticationComplete()
        } else {
            handlAuthenticationCanceled()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_AUTHORIZATION_STARTED, authorizationStarted)
    }

    private fun extractState(state: Bundle?) {
        if (state == null) {
            finish()
            return
        }

        authorizationStarted = state.getBoolean(KEY_AUTHORIZATION_STARTED, false)
    }

    private fun handleAuthenticationComplete() {
        when (intent.data?.path) {
            "/invalid_account" -> {
                val intent = AccountStateActivity.intent(this, intent.data)
                startActivity(intent)
                finish()
            }
            "/authentication" -> {
                val jwt = intent.data?.getQueryParameter("token") ?: ""
                val intent = Intent()
                intent.putExtra(EXTRA_IDP_TOKEN, jwt)
                setResult(RESULT_OK, intent)
                finish()
            }
            else -> {
                handlAuthenticationCanceled()
            }
        }
    }

    private fun handlAuthenticationCanceled() {
        finish()
    }

    private fun primaryColor(): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute (R.attr.colorPrimary, typedValue, true)
        return typedValue.data
    }

    private fun backButtonIcon(): Bitmap {
        return AppCompatResources.getDrawable(applicationContext, R.drawable.ic_arrow_back_white_24dp)!!.toBitmap()
    }
}