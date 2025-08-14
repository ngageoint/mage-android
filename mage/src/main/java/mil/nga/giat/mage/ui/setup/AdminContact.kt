package mil.nga.giat.mage.ui.setup

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dagger.hilt.android.qualifiers.ApplicationContext
import mil.nga.giat.mage.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
data class AdminContact @Inject constructor(
   val preferences: SharedPreferences,
   @ApplicationContext val context: Context
) {
   val email = preferences.getString(context.getString(R.string.contactInfoEmailKey), context.getString(R.string.contactInfoEmailDefaultValue))
   val phone = preferences.getString(context.getString(R.string.contactInfoPhoneKey), context.getString(R.string.contactInfoPhoneDefaultValue))
}

data class EmailState(
   val subject: String,
   val body: String
)

@Composable
fun AdminContact(
   text: String,
   contact: AdminContact,
   style: SpanStyle,
   emailState: EmailState
) {
   val context = LocalContext.current

   Column(
      Modifier.padding(vertical = 16.dp)
   ) {
      val split = text.split("administrator")
      val annotatedString = buildAnnotatedString {
         withStyle(style) {
            append("${split.first()}administrator")
         }

         if (contact.phone != null || contact.email != null) {
            withStyle(style) { append(" at ") }
         }

         if (contact.phone != null) {
            pushStringAnnotation(
               tag = "phone",
               annotation = contact.phone
            )
            withStyle(
               style = MaterialTheme.typography.bodyLarge.copy(
                  color = MaterialTheme.colorScheme.tertiary
               ).toSpanStyle()
            ) {
               append(contact.phone)
            }
            pop()
         }

         if (contact.phone != null && contact.email != null) {
            withStyle(style) { append(" or ") }
         }

         if (contact.email != null) {
            pushStringAnnotation(
               tag = "email",
               annotation = contact.email
            )
            withStyle(
               style = MaterialTheme.typography.bodyLarge.copy(
                  color = MaterialTheme.colorScheme.tertiary
               ).toSpanStyle()
            ) {
               append(contact.email)
            }
            pop()
         }

         withStyle(style) { append(split.last()) }
      }

      ClickableText(
         text = annotatedString,
         style = TextStyle(textAlign = TextAlign.Center),
         onClick = { offset ->
            annotatedString.getStringAnnotations(
               tag = "email", start = offset, end = offset
            ).firstOrNull()?.let {
               val uri = Uri.Builder()
                  .scheme("mailto")
                  .opaquePart(contact.email)
                  .appendQueryParameter("subject", emailState.subject)
                  .appendQueryParameter("body", emailState.body)
                  .build()

               val intent = Intent(Intent.ACTION_SENDTO, uri)
               context.startActivity(intent)
            }

            annotatedString.getStringAnnotations(
               tag = "phone", start = offset, end = offset
            ).firstOrNull()?.let {
               val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}"))
               context.startActivity(intent)
            }
         }
      )
   }
}