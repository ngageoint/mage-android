package mil.nga.giat.mage.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import mil.nga.giat.mage.ui.theme.MageTheme3
import mil.nga.giat.mage.R
import mil.nga.giat.mage.data.datasource.authentication.AuthenticationStrategy
import mil.nga.giat.mage.data.datasource.authentication.AuthenticationStrategyType
import mil.nga.giat.mage.ui.theme.onSurfaceDisabled

@Composable
fun LoginScreen(
    showProgress: MutableState<Boolean>,
    onServerUrlClick: () -> Unit,
    onLoginClick: (strategy: AuthenticationStrategyType, username: String, password: String) -> Unit,
    onIdpLoginClick: (strategy: AuthenticationStrategyType) -> Unit,
    onSignUpClick: () -> Unit,
    serverUrl: String?,
    version: String?,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.authenticationState.collectAsState(null)
    state?.let {
        LoginContent(
            state = it,
            showProgress = showProgress,
            onServerUrlClick = onServerUrlClick,
            onLoginClick = onLoginClick,
            onIdpLoginClick = onIdpLoginClick,
            onSignUpClick = onSignUpClick,
            serverUrl = serverUrl,
            version = version
        )
    }
}

@Composable
fun LoginContent(
    state: AuthenticationState,
    showProgress: MutableState<Boolean>,
    onServerUrlClick: () -> Unit,
    onLoginClick: (strategy: AuthenticationStrategyType, username: String, password: String) -> Unit,
    onIdpLoginClick: (strategy: AuthenticationStrategyType) -> Unit,
    onSignUpClick: () -> Unit,
    serverUrl: String?,
    version: String?,
) {
    MageTheme3 {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .safeDrawingPadding()
                    .padding(16.dp)
            ) {
                Spacer(modifier = Modifier.weight(1f, fill = true))

                if (state.hasSignInMethods) {
                    Title()
                    Spacer(Modifier.height(48.dp))
                    AuthenticationStrategies(
                        state = state,
                        onLoginClick = onLoginClick,
                        onIdpLoginClick = onIdpLoginClick,
                        onSignUpClick = onSignUpClick
                    )
                } else {
                    ServerConfigurationError()
                }

                Spacer(modifier = Modifier.weight(1f, fill = true))

                ServerInformation(
                    version = version,
                    serverUrl = serverUrl,
                    onServerUrlClick = onServerUrlClick
                )
            }

            if (showProgress.value) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f))
                        .clickable(enabled = true, onClick = {}),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(120.dp),
                        color = colorResource(R.color.icon),
                        strokeWidth = 8.dp
                    )
                }
            }
        }
    }
}

@Composable
fun Title() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_wand_blue),
            contentDescription = stringResource(id = R.string.sign_in_account),
            modifier = Modifier.size(60.dp)
        )
        Text(
            text = stringResource(id = R.string.sign_in_account),
            fontSize = 30.sp,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
fun AuthenticationStrategies(
    state: AuthenticationState,
    onLoginClick: (strategy: AuthenticationStrategyType, username: String, password: String) -> Unit,
    onIdpLoginClick: (strategy: AuthenticationStrategyType) -> Unit,
    onSignUpClick: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        if (state.local != null) {
            UsernamePasswordForm(state.local, onLoginClick)
            SignUpSection(onSignUpClick)
        }

        if (state.local != null && state.identityProviders.isNotEmpty()) {
            AuthDivider(Modifier.padding(vertical = 24.dp))
        }

        state.identityProviders.forEach { strategy ->
            if (strategy.type == AuthenticationStrategyType.LDAP) {
                UsernamePasswordForm(strategy, onLoginClick)
            } else {
                IdpButton(strategy, onClick = { onIdpLoginClick(strategy.type) })
            }
        }
    }
}

@Composable
fun ServerInformation(
    version: String?,
    serverUrl: String?,
    onServerUrlClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = serverUrl ?: "",
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clickable { onServerUrlClick() }
                .fillMaxWidth()
        )

        version?.let { AppVersion(it) }
    }
}

@Composable
fun ServerConfigurationError() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = colorResource(id = R.color.md_red_900),
            modifier = Modifier.size(96.dp)
        )

        Spacer(Modifier.height(32.dp))

        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
            Text(
                text = stringResource(R.string.login_error_title),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineLarge
            )
        }

        Spacer(Modifier.height(16.dp))

        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceDisabled) {
            Text(
                text = stringResource(R.string.login_error_message),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun SignUpSection(onSignUpClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.new_to_mage),
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(id = R.string.signup),
            fontSize = 18.sp,
            color = colorResource(id = R.color.md_blue_600),
            modifier = Modifier
                .clickable { onSignUpClick() }
                .padding(8.dp)
        )
    }
}

@Composable
fun AuthDivider(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                thickness = 1.dp,
                color = colorResource(id = R.color.divider)
            )

            Text(
                text = stringResource(id = R.string.or),
                modifier = Modifier
                    .weight(0.3f)
                    .padding(horizontal = 8.dp),
                textAlign = TextAlign.Center
            )

            HorizontalDivider(
                modifier = Modifier.weight(1f),
                thickness = 1.dp,
                color = colorResource(id = R.color.divider)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenAllPreview() {
    val state = AuthenticationState(
        local = AuthenticationStrategy(
            type = AuthenticationStrategyType.LOCAL,
            title = "Username/Password"
        ),
        identityProviders = mutableListOf<AuthenticationStrategy>().apply {
            add(AuthenticationStrategy(AuthenticationStrategyType.OAUTH, "OAuth"))
            add(AuthenticationStrategy(type = AuthenticationStrategyType.LDAP, title = "LDAP"))
        }
    )

    LoginContent(
        state = state,
        remember { mutableStateOf(false) },
        onServerUrlClick = {},
        onLoginClick = { _, _, _ -> },
        onIdpLoginClick = {},
        onSignUpClick = {},
        serverUrl = "demo.mage.com",
        version = "7.2.6",
    )
}

@Preview(showBackground = true)
@Composable
fun LoginScreenLocalPreview() {
    val state = AuthenticationState(
        local = AuthenticationStrategy(
            type = AuthenticationStrategyType.LOCAL,
            title = "Username/Password"
        )
    )

    LoginContent(
        state,
        remember { mutableStateOf(false) },
        {},
        { _, _, _ -> },
        {},
        {},
        serverUrl = "demo.mage.com",
        version = "7.2.6"
    )
}

@Preview(showBackground = true)
@Composable
fun LoginScreenLdapPreview() {
    val state = AuthenticationState(
        local = AuthenticationStrategy(
            type = AuthenticationStrategyType.LOCAL,
            title = "Username/Password"
        ),
        identityProviders = mutableListOf<AuthenticationStrategy>().apply {
            add(AuthenticationStrategy(type = AuthenticationStrategyType.LDAP, title = "LDAP"))
        }
    )

    LoginContent(
        state,
        remember { mutableStateOf(false) },
        {},
        { _, _, _ -> },
        {},
        {},
        serverUrl = "demo.mage.com",
        version = "7.2.6"
    )
}

@Preview(showBackground = true)
@Composable
fun LoginScreenLoadingPreview() {
    val state = AuthenticationState(
        local = AuthenticationStrategy(
            type = AuthenticationStrategyType.LOCAL,
            title = "Username/Password"
        )
    )

    LoginContent(
        state,
        remember { mutableStateOf(true) },
        {},
        { _, _, _ -> },
        {},
        {},
        serverUrl = "demo.mage.com",
        version = "7.2.6",
    )
}

