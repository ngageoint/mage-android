package mil.nga.giat.mage.ui.setup

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import mil.nga.giat.mage.R

sealed class AccountState(
   val title: String,
   val message: String
) {

   class Inactive(context: Context): AccountState(
      title = context.getString(R.string.account_inactive_title),
      message = context.getString(R.string.account_inactive_message)
   )

   class Disabled(context: Context): AccountState(
      title = context.getString(R.string.account_disabled_title),
      message = context.getString(R.string.account_disabled_message)
   )

   class Unknown(context: Context): AccountState(
      title = context.getString(R.string.account_unknown_title),
      message = context.getString(R.string.account_unknown_message)
   )
}

@Composable
fun AccountStateScreen(
   accountState: AccountState,
   onDone: () -> Unit,
   viewModel: AccountStateViewModel = hiltViewModel()
) {
   val contact = viewModel.contact

   Column(
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
         .fillMaxSize()
         .padding(horizontal = 16.dp)

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
               text = accountState.title,
               style = MaterialTheme.typography.displaySmall,
               modifier = Modifier.padding(bottom = 4.dp)
            )
         }
      }

      Column(
         horizontalAlignment = Alignment.CenterHorizontally,
         modifier = Modifier.weight(2f)
      ) {

         AdminContact(
            text = accountState.message,
            contact = contact,
            style = MaterialTheme.typography.bodyLarge.copy(
               color = LocalContentColor.current.copy(alpha = .87f)
            ).toSpanStyle(),
            emailState = EmailState(
               subject = "MAGE Account",
               body = accountState.message
            )
         )

         Button(
            onClick = { onDone() },
            modifier = Modifier
               .fillMaxWidth()
               .padding(top = 48.dp)
         ) {
            Text("OK")
         }
      }
   }
}