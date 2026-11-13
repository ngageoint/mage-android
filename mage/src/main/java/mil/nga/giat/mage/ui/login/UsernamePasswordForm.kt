package mil.nga.giat.mage.ui.login

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mil.nga.giat.mage.R
import mil.nga.giat.mage.data.datasource.authentication.AuthenticationStrategy
import mil.nga.giat.mage.data.datasource.authentication.AuthenticationStrategyType
import org.json.JSONObject

@Composable
fun UsernamePasswordForm(
    strategy: AuthenticationStrategy,
    onLoginClick: (strategy: AuthenticationStrategyType, username: String, password: String) -> Unit
) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    var isUserNameError by rememberSaveable { mutableStateOf(false) }
    var isPasswordError by rememberSaveable { mutableStateOf(false) }

    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val usernameHint = when (strategy.type) {
        AuthenticationStrategyType.LDAP -> stringResource(id = R.string.ldap_username)
        else -> stringResource(id = R.string.username)
    }

    val passwordHint = when (strategy.type) {
        AuthenticationStrategyType.LDAP -> stringResource(id = R.string.ldap_password)
        else -> stringResource(id = R.string.password)
    }

    val focusManager = LocalFocusManager.current

    val login = {
        focusManager.clearFocus()

        username = username.trim()
        password = password.trim()

        if (username.isEmpty()) {
            isUserNameError = true
        } else if (password.isEmpty()) {
            isPasswordError = true
        } else {
            onLoginClick(strategy.type, username.lowercase(), password)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = username,
            onValueChange = { value ->
                username = value
                if (username.isNotEmpty()) {
                    isUserNameError = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            label = { Text(usernameHint) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            isError = isUserNameError,
            supportingText = {
                if (isUserNameError) {
                    Text(
                        text = stringResource(R.string.login_username_blank),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { value ->
                password = value
                if (password.isNotEmpty()) {
                    isPasswordError = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            label = { Text(passwordHint) },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    login()
                }
            ),
            trailingIcon = {
                val image =
                    if (passwordVisible)
                        Icons.Filled.Visibility
                    else
                        Icons.Filled.VisibilityOff

                val description =
                    if (passwordVisible) stringResource(R.string.hide_password) else stringResource(
                        R.string.show_password
                    )

                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, description)
                }
            },
            isError = isPasswordError,
            supportingText = {
                if (isPasswordError) {
                    Text(
                        text = stringResource(R.string.login_password_blank),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        LoginButton(strategy = strategy, onClick = login)
    }
}

@Composable
fun LoginButton(
    strategy: AuthenticationStrategy,
    onClick: () -> Unit
) {
    val themeColors = ButtonDefaults.buttonColors()
    val colors = if (strategy.type == AuthenticationStrategyType.LOCAL) {
        themeColors
    } else {
        ButtonDefaults.buttonColors(
            containerColor = strategy.containerColor ?: themeColors.containerColor,
            contentColor = strategy.contentColor ?: themeColors.contentColor
        )
    }

    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = colors
    ) {

        Text(
            text = if (strategy.type == AuthenticationStrategyType.LOCAL) stringResource(R.string.sign_in) else signInText(strategy),
            style = MaterialTheme.typography.labelMedium
        )
    }

    // Leave a gap below the button so the next sign-in method doesn't touch it.
    Spacer(modifier = Modifier.height(16.dp))
}

@Preview(showBackground = true)
@Composable
fun UsernamePasswordFormLocalPreview() {
    val strategy = AuthenticationStrategy.parse(JSONObject().apply {
        put("type", AuthenticationStrategyType.LOCAL.type)
        put("title", "LDAP")
    })!!

    UsernamePasswordForm(strategy, onLoginClick = { _, _, _ -> })
}

@Preview(showBackground = true)
@Composable
fun UsernamePasswordFormLdapPreview() {
    val strategy = AuthenticationStrategy.parse(JSONObject().apply {
        put("type", AuthenticationStrategyType.LDAP.type)
        put("title", "LDAP")
        put("textColor", "#24de32")
        put("buttonColor", "#1E22E5")
    })!!

    UsernamePasswordForm(strategy, onLoginClick = { _, _, _ -> })
}