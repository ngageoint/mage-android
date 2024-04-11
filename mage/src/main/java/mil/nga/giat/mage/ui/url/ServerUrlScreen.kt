package mil.nga.giat.mage.ui.url

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SecurityUpdateWarning
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import mil.nga.giat.mage.R
import mil.nga.giat.mage.login.ServerUrlViewModel
import mil.nga.giat.mage.login.UrlState
import mil.nga.giat.mage.ui.theme.onSurfaceDisabled

@Composable
fun ServerUrlScreen(
   onDone: () -> Unit,
   onCancel: (() -> Unit)? = null,
   viewModel: ServerUrlViewModel = hiltViewModel()
) {
   val scrollState = rememberScrollState()
   var url by remember { mutableStateOf(viewModel.url) }
   val urlState by viewModel.urlState.observeAsState()
   val unsavedData by viewModel.unsavedData.observeAsState(false)
   var errorState by remember { mutableStateOf<UrlState.Error?>(null) }
   val keyboardController = LocalSoftwareKeyboardController.current

   LaunchedEffect(urlState) {
      if (urlState is UrlState.Valid) {
         onDone()
      }
   }

   if (unsavedData) {
      UnsavedDataDialog(
         onDismiss = { onCancel?.invoke() },
         onContinue = { viewModel.confirmUnsavedData() }
      )
   }

   errorState?.let { state ->
      ErrorDialog(state) { errorState = null }
   }

   if (urlState is UrlState.InProgress) {
      LinearProgressIndicator(Modifier.fillMaxWidth())
   }

   Column(
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
         .fillMaxSize()
         .padding(horizontal = 16.dp)
         .verticalScroll(scrollState),

   ) {

      Column(
         verticalArrangement = Arrangement.Center,
         horizontalAlignment = Alignment.CenterHorizontally,
         modifier = Modifier.weight(1f)
      ) {
         Icon(
            painter = painterResource(R.drawable.ic_wand_white_50dp),
            contentDescription = "wand",
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier
               .padding(bottom = 16.dp)
               .size(72.dp)
         )

         CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            Text(
               text = "Welcome to MAGE!",
               style = MaterialTheme.typography.displaySmall,
               modifier = Modifier.padding(bottom = 4.dp)
            )
         }

         CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceDisabled) {
            Text(
               text = "Specify a MAGE server URL to get started",
               style = MaterialTheme.typography.titleMedium
            )
         }
      }

      Column(
         horizontalAlignment = Alignment.CenterHorizontally,
         verticalArrangement = Arrangement.Center,
         modifier = Modifier.weight(1f)
      ) {
         TextField(
            label = { Text("MAGE Server URL") },
            value = url,
            onValueChange = { url = it },
            enabled = urlState != UrlState.InProgress,
            keyboardOptions = KeyboardOptions(
               keyboardType = KeyboardType.Uri,
               imeAction = ImeAction.Go
            ),
            keyboardActions = KeyboardActions(
               onGo = {
                  keyboardController?.hide()
                  viewModel.checkUrl(url)
               }
            ),
            isError = urlState is UrlState.Error || urlState is UrlState.Incompatible || urlState is UrlState.Invalid,
            supportingText = {
               if (urlState is UrlState.Invalid) {
                  Text("Please enter a valid URL")
               }
            },
            modifier = Modifier
               .fillMaxWidth()
               .padding(bottom = 8.dp)
         )

         Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
         ) {
            if (url.isNotEmpty()) {
               OutlinedButton(
                  onClick = { onCancel?.invoke() },
                  modifier = Modifier.weight(1f)
               ) {
                  Text("Cancel")
               }
            }

            Button(
               onClick = {
                  keyboardController?.hide()
                  viewModel.checkUrl(url)
               },
               enabled = urlState != UrlState.InProgress,
               modifier = Modifier.weight(1f)
            ) {
               Text("Set URL")
            }
         }
      }

      Column(
         verticalArrangement = Arrangement.SpaceBetween,
         horizontalAlignment = Alignment.CenterHorizontally,
         modifier = Modifier.weight(1f)
      ) {
         when (val state = urlState) {
            is UrlState.Incompatible -> { Incompatible(state) }
            is UrlState.Error -> {
               ErrorContent(state.message) { errorState = state }
            }
            else -> { Spacer(Modifier.weight(1f)) }
         }

         viewModel.version?.let{ version ->
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceDisabled) {
               Text(
                  text = "MAGE Android $version",
                  style = MaterialTheme.typography.bodySmall,
                  modifier = Modifier.padding(bottom = 16.dp)
               )
            }
         }
      }
   }
}

@Composable
private fun Incompatible(
   state: UrlState.Incompatible
) {
   val context = LocalContext.current

   Column(
      Modifier.padding(vertical = 16.dp)
   ) {
      Icon(
         Icons.Outlined.SecurityUpdateWarning,
         contentDescription = "Incompatible",
         tint = MaterialTheme.colorScheme.error,
         modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(bottom = 16.dp)
            .size(36.dp)
      )

      val text = buildAnnotatedString {
         val textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = LocalContentColor.current.copy(alpha = .87f)
         ).toSpanStyle()
         withStyle(textStyle) {
            append("Your MAGE application is not compatible with server version ${state.version}.  Please update your application or contact your MAGE administrator")
         }

         if (state.contactInfo.phone != null || state.contactInfo.email != null) {
            withStyle(textStyle) { append(" at ") }
         }

         if (state.contactInfo.phone != null) {
            pushStringAnnotation(
               tag = "phone",
               annotation = state.contactInfo.phone
            )
            withStyle(
               style = MaterialTheme.typography.bodyLarge.copy(
                  color = MaterialTheme.colorScheme.tertiary
               ).toSpanStyle()
            ) {
               append(state.contactInfo.phone)
            }
            pop()
         }

         if (state.contactInfo.phone != null && state.contactInfo.email != null) {
            withStyle(textStyle) { append(" or ") }
         }

         if (state.contactInfo.email != null) {
            pushStringAnnotation(
               tag = "email",
               annotation = state.contactInfo.email
            )
            withStyle(
               style = MaterialTheme.typography.bodyLarge.copy(
                  color = MaterialTheme.colorScheme.tertiary
               ).toSpanStyle()
            ) {
               append(state.contactInfo.email)
            }
            pop()
         }

         withStyle(textStyle) { append(" for support.") }
      }

      ClickableText(
         text = text,
         style = TextStyle(textAlign = TextAlign.Center),
         onClick = { offset ->
            text.getStringAnnotations(
               tag = "email", start = offset, end = offset
            ).firstOrNull()?.let {
               val uri =  Uri.Builder()
                  .scheme("mailto")
                  .opaquePart(state.contactInfo.email)
                  .appendQueryParameter("subject", "MAGE Compatibility")
                  .appendQueryParameter("body", "MAGE Application not compatible with server version ${state.version}")
                  .build()

               val intent = Intent(Intent.ACTION_SENDTO, uri)
               context.startActivity(intent)
            }

            text.getStringAnnotations(
               tag = "phone", start = offset, end = offset
            ).firstOrNull()?.let {
               val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${state.contactInfo.phone}"))
               context.startActivity(intent)
            }
         }
      )
   }
}

@Composable
private fun ErrorContent(
   message: String?,
   onInfo: (String) -> Unit
) {
   Column(Modifier.padding(vertical = 16.dp)) {
      Icon(
         Icons.Outlined.ErrorOutline,
         contentDescription = "Error",
         tint = MaterialTheme.colorScheme.error,
         modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(bottom = 16.dp)
            .size(32.dp)
      )

      val text = buildAnnotatedString {
         val style = MaterialTheme.typography.bodyLarge.copy(
            color = LocalContentColor.current.copy(alpha = .87f)
         ).toSpanStyle()
         withStyle(style) { append("This URL does not appear to be a MAGE server") }

         if (message != null) {
            pushStringAnnotation(
               tag = "info",
               annotation = "info"
            )
            withStyle(
               style = MaterialTheme.typography.bodyLarge.copy(
                  color = MaterialTheme.colorScheme.tertiary
               ).toSpanStyle()
            ) {
               append(" more info")
            }
            pop()
         }

         withStyle(style) { append(".") }
      }

      ClickableText(
         text = text,
         style = TextStyle(textAlign = TextAlign.Center),
         onClick = { offset ->
            if (message != null) {
               text.getStringAnnotations(
                  tag = "info", start = offset, end = offset
               ).firstOrNull()?.let {
                  onInfo(message)
               }
            }
         }
      )
   }
}

@Composable
fun UnsavedDataDialog(
   onContinue: () -> Unit,
   onDismiss: () -> Unit
) {
   AlertDialog(
      icon = {
         Icon(
            imageVector = Icons.Outlined.WarningAmber,
            tint = MaterialTheme.colorScheme.error,
            contentDescription = "warning"
         )
      },
      title = {
         Text(text = "Unsaved Data")
      },
      text = {
         Text(
            text = "All data will be lost if you continue.",
            textAlign = TextAlign.Center
         )
      },
      onDismissRequest = { onDismiss() },
      dismissButton = {
         TextButton(onClick = { onDismiss() }) {
            Text("Cancel")
         }
      },
      confirmButton = {
         TextButton(onClick = { onContinue() }) {
            Text("Continue")
         }
      }
   )
}

@Composable
fun ErrorDialog(
   errorState: UrlState.Error,
   onDismiss: () -> Unit
) {
   AlertDialog(
      icon = {
         Icon(
            imageVector = Icons.Outlined.Info,
            tint = MaterialTheme.colorScheme.tertiary,
            contentDescription = "info"
         )
      },
      title = {
         errorState.statusCode?.let {
            Text(
               text = it.toString(),
               textAlign = TextAlign.Center
            )
         }
      },
      text = {
         errorState.message?.let {
            SelectionContainer {
               Text(
                  text = it,
                  textAlign = TextAlign.Center,
                  fontFamily = FontFamily.Monospace
               )
            }
         }
      },
      onDismissRequest = { onDismiss() },
      confirmButton = {
         TextButton(onClick = { onDismiss() }) {
            Text("OK")
         }
      }
   )
}