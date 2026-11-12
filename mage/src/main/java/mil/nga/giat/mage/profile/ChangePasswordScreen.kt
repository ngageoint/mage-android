package mil.nga.giat.mage.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import mil.nga.giat.mage.R
import mil.nga.giat.mage.ui.theme.MageTheme3

@Composable
fun ChangePasswordScreen(
   onNavigateUp: () -> Unit,
   onPasswordChanged: () -> Unit,
   viewModel: ChangePasswordViewModel = hiltViewModel()
) {
   val uiState by viewModel.uiState.collectAsState()

   ChangePasswordScreenContent(
      uiState = uiState,
      onNavigateUp = onNavigateUp,
      onPasswordChanged = viewModel::onPasswordChanged,
      onNewPasswordChanged = viewModel::onNewPasswordChanged,
      onNewPasswordConfirmChanged = viewModel::onNewPasswordConfirmChanged,
      onSubmit = viewModel::changePassword,
      onResultHandled = viewModel::onResultHandled,
      onSuccessDismissed = onPasswordChanged
   )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangePasswordScreenContent(
   uiState: ChangePasswordUiState,
   onNavigateUp: () -> Unit,
   onPasswordChanged: (String) -> Unit,
   onNewPasswordChanged: (String) -> Unit,
   onNewPasswordConfirmChanged: (String) -> Unit,
   onSubmit: () -> Unit,
   onResultHandled: () -> Unit,
   onSuccessDismissed: () -> Unit
) {
   MageTheme3 {
      Scaffold(
         topBar = {
            TopAppBar(
               title = { Text(stringResource(R.string.change_password)) },
               navigationIcon = {
                  IconButton(onClick = onNavigateUp) {
                     Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                  }
               },
               modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top))
            )
         }
      ) { paddingValues ->
         Column(
            modifier = Modifier
               .padding(paddingValues)
               .fillMaxSize()
               .verticalScroll(rememberScrollState())
               .padding(horizontal = 24.dp, vertical = 16.dp)
         ) {
            PasswordField(
               value = uiState.password,
               onValueChange = onPasswordChanged,
               label = stringResource(R.string.current_password),
               error = uiState.passwordError,
               imeAction = ImeAction.Next
            )

            PasswordField(
               value = uiState.newPassword,
               onValueChange = onNewPasswordChanged,
               label = stringResource(R.string.new_password),
               error = uiState.newPasswordError,
               imeAction = ImeAction.Next,
               modifier = Modifier.padding(top = 8.dp)
            )

            uiState.passwordStrength?.let { strength ->
               PasswordStrengthIndicator(strength, Modifier.padding(top = 4.dp))
            }

            PasswordField(
               value = uiState.newPasswordConfirm,
               onValueChange = onNewPasswordConfirmChanged,
               label = stringResource(R.string.re_type_new_password),
               error = uiState.newPasswordConfirmError,
               imeAction = ImeAction.Done,
               modifier = Modifier.padding(top = 8.dp)
            )

            Button(
               onClick = onSubmit,
               enabled = !uiState.isSubmitting,
               modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 16.dp)
            ) {
               Text(stringResource(R.string.update_password))
            }
         }
      }

      when (uiState.result) {
         ChangePasswordResult.Success -> {
            AlertDialog(
               onDismissRequest = {},
               title = { Text("Password Changed") },
               text = { Text("Your password has been changed, for security purposes you will need to login with your new password.") },
               confirmButton = {
                  TextButton(onClick = {
                     onResultHandled()
                     onSuccessDismissed()
                  }) {
                     Text(stringResource(android.R.string.ok))
                  }
               }
            )
         }
         ChangePasswordResult.InvalidPassword -> {
            // This will show up inline with the password fields
         }
         ChangePasswordResult.NoConnection -> {
            AlertDialog(
               onDismissRequest = onResultHandled,
               title = { Text("No connection") },
               text = { Text("Please ensure you have an internet connection and try again") },
               confirmButton = {
                  TextButton(onClick = onResultHandled) {
                     Text(stringResource(android.R.string.ok))
                  }
               }
            )
         }
         is ChangePasswordResult.Error -> {
            AlertDialog(
               onDismissRequest = onResultHandled,
               title = { Text("Error changing password") },
               text = { Text(uiState.result.message ?: "") },
               confirmButton = {
                  TextButton(onClick = onResultHandled) {
                     Text(stringResource(android.R.string.ok))
                  }
               }
            )
         }
         null -> {}
      }
   }
}

@Composable
private fun PasswordField(
   value: String,
   onValueChange: (String) -> Unit,
   label: String,
   error: String?,
   imeAction: ImeAction,
   modifier: Modifier = Modifier
) {
   var isVisible by remember { mutableStateOf(false) }

   Column(modifier = modifier.fillMaxWidth()) {
      OutlinedTextField(
         value = value,
         onValueChange = onValueChange,
         label = { Text(label) },
         singleLine = true,
         isError = error != null,
         visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
         keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
         trailingIcon = {
            IconButton(onClick = { isVisible = !isVisible }) {
               Icon(
                  if (isVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                  contentDescription = if (isVisible) "Hide password" else "Show password"
               )
            }
         },
         modifier = Modifier.fillMaxWidth()
      )

      error?.let {
         Text(
            text = it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
         )
      }
   }
}

@Composable
private fun PasswordStrengthIndicator(strength: PasswordStrength, modifier: Modifier = Modifier) {
   val color = when (strength) {
      PasswordStrength.WEAK -> colorResource(R.color.md_red_500)
      PasswordStrength.FAIR -> colorResource(R.color.md_orange_500)
      PasswordStrength.GOOD -> colorResource(R.color.md_amber_500)
      PasswordStrength.STRONG -> colorResource(R.color.md_blue_500)
      PasswordStrength.EXCELLENT -> colorResource(R.color.md_green_500)
   }

   Column(modifier = modifier.fillMaxWidth()) {
      LinearProgressIndicator(
         progress = { (strength.ordinal + 1) / 5f },
         color = color,
         modifier = Modifier.fillMaxWidth()
      )
      Text(
         text = strength.label,
         color = color,
         style = MaterialTheme.typography.bodySmall,
         modifier = Modifier.padding(top = 4.dp)
      )
   }
}
