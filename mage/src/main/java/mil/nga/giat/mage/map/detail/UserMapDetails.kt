package mil.nga.giat.mage.map.detail

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mil.nga.giat.mage.map.UserMapState
import mil.nga.sf.Geometry

sealed class UserAction {
   class Email(val user: UserMapState): UserAction()
   class Phone(val user: UserMapState): UserAction()
   class Directions(val id: Long, val geometry: Geometry, val icon: Any?): UserAction()
   class Location(val geometry: Geometry): UserAction()
   class Details(val id: Long): UserAction()
}

@Composable
fun UserMapDetails(
   userState: UserMapState?,
   onAction: ((UserAction) -> Unit)? = null
) {
   if (userState != null) {
      FeatureDetails(
         userState,
         actions = {
            UserMapActions(
               userState,
               onEmail = {
                  onAction?.invoke(UserAction.Email(userState))
               },
               onPhone = {
                  onAction?.invoke(UserAction.Phone(userState))
               }
            )
         },
         onAction = { action ->
            when (action) {
               is FeatureAction.Details<*> -> {
                  onAction?.invoke(UserAction.Details(userState.id))
               }
               is FeatureAction.Directions<*> -> {
                  onAction?.invoke(UserAction.Directions(userState.id, action.geometry, action.image))
               }
               is FeatureAction.Location -> {
                  onAction?.invoke(UserAction.Location(action.geometry))
               }
            }
         }
      )
   }
}

@Composable
private fun UserMapActions(
   user: UserMapState,
   onEmail: () -> Unit,
   onPhone: () -> Unit
) {
   Row {
      if (user.email?.isNotEmpty() == true) {
         IconButton(
            modifier = Modifier.padding(end = 8.dp),
            onClick = { onEmail() }
         ) {
            Icon(
               imageVector = Icons.Outlined.Email,
               contentDescription = "Email",
               tint = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
            )
         }
      }

      if (user.phone?.isNotEmpty() == true) {
         IconButton(
            modifier = Modifier.padding(end = 8.dp),
            onClick = { onPhone() }
         ) {
            Icon(
               imageVector = Icons.Outlined.Phone,
               contentDescription = "Phone",
               tint = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
            )
         }
      }
   }
}