package mil.nga.giat.mage.login.idp

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.TypedValue
import androidx.appcompat.content.res.AppCompatResources
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.FragmentActivity
import mil.nga.giat.mage.R
import androidx.core.net.toUri
import mil.nga.giat.mage.data.datasource.authentication.AuthenticationStrategyType
import mil.nga.giat.mage.login.AccountStateActivity

data class IdpLoginRequest(
    val serverUrl: String,
    val strategy: AuthenticationStrategyType
) {

    companion object {
        fun fromIntent(intent: Intent): IdpLoginRequest? {
            val serverUrl = intent.getStringExtra(IdpLoginActivity.EXTRA_SERVER_URL) ?: return null
            val strategy = intent.getStringExtra(IdpLoginActivity.EXTRA_IDP_STRATEGY)
                ?.let(AuthenticationStrategyType::fromType)
                ?: return null

            return IdpLoginRequest(serverUrl = serverUrl, strategy = strategy)
        }

        fun toIntent(
            context: Context,
            serverUrl: String,
            strategy: AuthenticationStrategyType
        ): Intent =
            Intent(context, IdpLoginActivity::class.java).apply {
                putExtra(IdpLoginActivity.EXTRA_SERVER_URL, serverUrl)
                putExtra(IdpLoginActivity.EXTRA_IDP_STRATEGY, strategy.type)
            }
    }
}

class IdpLoginActivity : FragmentActivity() {
    companion object {
        private const val KEY_AUTHORIZATION_STARTED = "AUTHORIZATION_STARTED"

        const val EXTRA_SERVER_URL = "EXTRA_SERVER_URL"
        const val EXTRA_IDP_STRATEGY = "EXTRA_IDP_STRATEGY"
        const val EXTRA_IDP_TOKEN = "EXTRA_IDP_TOKEN"
    }

    private var authorizationStarted = false
    private lateinit var loginRequest: IdpLoginRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loginRequest = IdpLoginRequest.fromIntent(intent) ?: run {
            finish()
            return
        }

        authorizationStarted = savedInstanceState?.getBoolean(KEY_AUTHORIZATION_STARTED, false) ?: false
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

            // Prefer Chrome when it supports Custom Tabs, otherwise fall back to the default browser
            CustomTabsClient.getPackageName(this, listOf("com.android.chrome"), true)?.let {
                customTabsIntent.intent.setPackage(it)
            }

            val baseUrl = loginRequest.serverUrl.trim().removeSuffix("/")
            val idpUrl = baseUrl.toUri()
                .buildUpon()
                .appendPath("auth")
                .appendPath(loginRequest.strategy.type)
                .appendPath("signin")
                .appendQueryParameter("state", "mobile")
                .build()
                .toString()

            customTabsIntent.launchUrl(this, idpUrl.toUri())

            authorizationStarted = true

            return
        }

        if (intent.data != null) {
            handleAuthenticationComplete()
        } else {
            handleAuthenticationCanceled()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_AUTHORIZATION_STARTED, authorizationStarted)
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
                intent.putExtra(EXTRA_IDP_STRATEGY, loginRequest.strategy.type)
                setResult(RESULT_OK, intent)
                finish()
            }
            else -> {
                handleAuthenticationCanceled()
            }
        }
    }

    private fun handleAuthenticationCanceled() {
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