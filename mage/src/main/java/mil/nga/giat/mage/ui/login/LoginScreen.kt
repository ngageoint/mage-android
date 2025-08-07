package mil.nga.giat.mage.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import mil.nga.giat.mage.R
import mil.nga.giat.mage.login.ServerAuthTypes
import mil.nga.giat.mage.login.ServerAuthTypesHelper
import mil.nga.giat.mage.ui.theme.MageTheme3
import mil.nga.giat.mage.ui.theme.onSurfaceDisabled
import org.json.JSONObject


@Composable
fun LoginScreen(
    authStrategiesJson: MutableState<JSONObject>,
    showProgress: MutableState<Boolean>,
    onServerUrlClick: () -> Unit,
    onLoginClick: (authTypes: ServerAuthTypes, userName: String, pwd: String) -> Unit,
    onIdpLoginClick: (authType: ServerAuthTypes) -> Unit,
    onSignUpClick: () -> Unit,
    serverUrl: String = "",
    version: String = ""
) {
    val sortedAuthTypesMap = ServerAuthTypesHelper.getAvailableServerAuthTypesMap(authStrategiesJson.value)

    MageTheme3 {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {

                //scroll container
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {

                    //header section
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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

                    //content section (login options, errors)
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {

                        if (sortedAuthTypesMap.isNotEmpty()) {
                            val authTypesSet = sortedAuthTypesMap.entries

                            authTypesSet.forEachIndexed { index, entry ->
                                when (entry.key) {
                                    ServerAuthTypes.LOCAL -> {
                                        AuthForm(null, entry.key, onLoginClick)
                                        SignUpSection(onSignUpClick)
                                    }

                                    ServerAuthTypes.LDAP ->
                                        AuthForm(entry.value, entry.key, onLoginClick)

                                    ServerAuthTypes.OAUTH,
                                    ServerAuthTypes.OPENIDCONNECT,
                                    ServerAuthTypes.SAML ->
                                        AuthStrategyButton(
                                            entry.value,
                                            onClick = { onIdpLoginClick(entry.key) })
                                }

                                if (index < authTypesSet.size - 1) {
                                    AuthDivider()
                                }
                            }
                        } else {
                            NoLoginMethodsAvailableError()
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }

                //footer section
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = serverUrl,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                            .clickable { onServerUrlClick() }
                            .padding(top = 8.dp)

                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    AppVersion(version)
                }
            }

            //wait indicator overlay
            if (showProgress.value) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.1f))
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
fun NoLoginMethodsAvailableError() {
    Row(
        modifier = Modifier.fillMaxWidth().scale(0.8f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_error_outline_white_24dp),
            contentDescription = stringResource(id = R.string.login_error_message),
            tint = colorResource(id = R.color.md_red_900),
            modifier = Modifier.size(20.sp.value.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.login_error_message),
            fontSize = 20.sp,
            textAlign = TextAlign.Center
        )
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
            text = stringResource(id = R.string.signup_here),
            fontSize = 18.sp,
            color = colorResource(id = R.color.md_blue_600),
            modifier = Modifier
                .clickable { onSignUpClick() }
                .padding(8.dp)
        )
    }
}


@Composable
fun AuthDivider() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f).padding(top = 10.dp),
            thickness = 1.dp,
            color = colorResource(id = R.color.divider)
        )

        Text(
            text = stringResource(id = R.string.or),
            modifier = Modifier.weight(0.3f).padding(horizontal = 8.dp),
            textAlign = TextAlign.Center
        )

        HorizontalDivider(
            modifier = Modifier.weight(1f).padding(top = 10.dp),
            thickness = 1.dp,
            color = colorResource(id = R.color.divider)
        )
    }
}

@Preview
@Composable
fun LoginScreenAllPreview() {
    val authTypesJson = JSONObject()
    authTypesJson.put(ServerAuthTypes.LOCAL.name, JSONObject().put("type", ServerAuthTypes.LOCAL.name))
    authTypesJson.put(ServerAuthTypes.LDAP.name, JSONObject().put("type", ServerAuthTypes.LDAP.name))
    authTypesJson.put(ServerAuthTypes.OAUTH.name, JSONObject().put("type", ServerAuthTypes.OAUTH.name))

    LoginScreen(remember {mutableStateOf(authTypesJson)}, remember {mutableStateOf(false)}, {}, { _, _, _ -> }, {}, {}, serverUrl = "demo.mage.com", version = "7.2.6")
}

@Preview
@Composable
fun LoginScreenLocalPreview() {
    val authTypesJson = JSONObject()
    authTypesJson.put(ServerAuthTypes.LOCAL.name, JSONObject().put("type", ServerAuthTypes.LOCAL.name))

    LoginScreen(remember {mutableStateOf(authTypesJson)}, remember {mutableStateOf(false)}, {}, { _, _, _ -> }, {}, {}, serverUrl = "demo.mage.com", version = "7.2.6")
}

@Preview
@Composable
fun LoginScreenLdapPreview() {
    val authTypesJson = JSONObject()
    authTypesJson.put(ServerAuthTypes.LDAP.name, JSONObject().put("type", ServerAuthTypes.LDAP.name))

    LoginScreen(remember {mutableStateOf(authTypesJson)}, remember {mutableStateOf(false)}, {}, { _, _, _ -> }, {}, {}, serverUrl = "demo.mage.com", version = "7.2.6")
}

@Preview
@Composable
fun LoginScreenLoadingPreview() {
    val authTypesJson = JSONObject()
    authTypesJson.put(ServerAuthTypes.LOCAL.name, JSONObject().put("type", ServerAuthTypes.LOCAL.name))

    LoginScreen(remember {mutableStateOf(authTypesJson)}, remember {mutableStateOf(true)}, {}, { _, _, _ -> }, {}, {}, serverUrl = "demo.mage.com", version = "7.2.6")
}

@Preview
@Composable
fun DividerPreview() {
    AuthDivider()
}


