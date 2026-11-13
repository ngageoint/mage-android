package mil.nga.giat.mage.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mil.nga.giat.mage.R
import mil.nga.giat.mage.data.datasource.authentication.AuthenticationStrategy
import mil.nga.giat.mage.data.datasource.authentication.AuthenticationStrategyType
import org.json.JSONObject

/**
 * Matches the web app: the admin's custom button text, else "Sign In with <title>",
 * else plain "Sign In" when the strategy has no title.
 */
@Composable
fun signInText(strategy: AuthenticationStrategy): String {
    return strategy.buttonText
        ?: strategy.title.trim().takeIf { it.isNotEmpty() }?.let { stringResource(R.string.sign_in_with, it) }
        ?: stringResource(R.string.sign_in)
}

@Composable
fun IdpButton(
    strategy: AuthenticationStrategy,
    onClick: () -> Unit
) {
    val title = signInText(strategy)
    val themeColors = ButtonDefaults.buttonColors()

    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = strategy.containerColor ?: themeColors.containerColor,
            contentColor = strategy.contentColor ?: themeColors.contentColor
        )
    ) {
        strategy.icon?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        }

        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium
        )
    }

    Spacer(modifier = Modifier.height(16.dp))
}

@Preview
@Composable
fun LocalAuthButtonPreview() {
    val strategy = AuthenticationStrategy.parse(JSONObject().apply {
        put("type", AuthenticationStrategyType.LOCAL.type)
        put("title", "MAGE")
    })!!

    MaterialTheme {
        IdpButton(strategy) {}
    }
}

@Preview
@Composable
fun LdapAuthButtonPreview() {
    val strategy = AuthenticationStrategy.parse(JSONObject().apply {
        put("type", AuthenticationStrategyType.LDAP.type)
        put("title", "LDAP")
        put("textColor", "#24de32")
        put("buttonColor", "#1E22E5")
    })!!

    MaterialTheme {
        IdpButton(strategy) {}
    }
}