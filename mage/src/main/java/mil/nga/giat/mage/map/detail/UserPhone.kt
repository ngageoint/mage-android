package mil.nga.giat.mage.map.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Phone
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.LiveData
import mil.nga.giat.mage.sdk.datastore.user.User
import mil.nga.giat.mage.ui.theme.MageTheme

sealed class UserPhoneAction {
   class Call(val user: User): UserPhoneAction()
   class Message(val user: User): UserPhoneAction()
   object Dismiss : UserPhoneAction()
}

@Composable
fun UserPhoneDetails(
   liveData: LiveData<User?>,
   onAction: ((UserPhoneAction) -> Unit)? = null
) {
   val userState by liveData.observeAsState()
   val openDialog = userState != null

   MageTheme {
      if (openDialog) {
         Dialog(
            onDismissRequest = { onAction?.invoke(UserPhoneAction.Dismiss) }
         ) {
            Surface(
               shape = MaterialTheme.shapes.medium,
               color =  MaterialTheme.colors.surface
            ) {
               Column(Modifier.padding(16.dp)) {
                  Text(
                     text = "Contact ${userState?.displayName}",
                     style = MaterialTheme.typography.h6,
                     modifier = Modifier.padding(bottom = 8.dp)
                  )

                  Text(
                     text = "${userState?.primaryPhone}",
                     style = MaterialTheme.typography.subtitle1,
                     modifier = Modifier.padding(bottom = 16.dp)
                  )

                  UserPhoneOptions(userState)
               }
            }
         }
      }
   }
}

@Composable
private fun UserPhoneOptions(
   user: User?,
   onAction: ((UserPhoneAction) -> Unit)? = null
) {
   if (user == null) return

   Column {
      Row(
         verticalAlignment = Alignment.CenterVertically,
         modifier = Modifier
            .fillMaxWidth()
            .clickable {
               onAction?.invoke(UserPhoneAction.Call(user))
            }
            .padding(vertical = 16.dp, horizontal = 16.dp)
      ) {
         CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Box(
               contentAlignment = Alignment.Center,
               modifier = Modifier
                  .height(48.dp)
                  .width(48.dp)
                  .clip(RoundedCornerShape(24.dp))
                  .background(Color.Gray)
            ) {
               Icon(
                  Icons.Default.Phone,
                  tint = Color.White,
                  contentDescription = "Phone"
               )
            }

            Text(
               text = "Talk",
               style = MaterialTheme.typography.subtitle1,
               fontWeight = FontWeight.Medium,
               modifier = Modifier.padding(start = 24.dp)
            )
         }
      }

      Row(
         verticalAlignment = Alignment.CenterVertically,
         modifier = Modifier
            .fillMaxWidth()
            .clickable {
               onAction?.invoke(UserPhoneAction.Message(user))
            }
            .padding(vertical = 16.dp, horizontal = 16.dp)
      ) {
         CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Box(
               contentAlignment = Alignment.Center,
               modifier = Modifier
                  .height(48.dp)
                  .width(48.dp)
                  .clip(RoundedCornerShape(24.dp))
                  .background(Color.Gray)
            ) {
               Icon(
                  Icons.Default.Message,
                  tint = Color.White,
                  contentDescription = "Message"
               )
            }

            Text(
               text = "Text",
               style = MaterialTheme.typography.subtitle1,
               fontWeight = FontWeight.Medium,
               modifier = Modifier.padding(start = 24.dp)
            )
         }
      }
   }
}