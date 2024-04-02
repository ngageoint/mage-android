package mil.nga.giat.mage.ui.map.sheet

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mil.nga.giat.mage.map.UserMapState
import mil.nga.giat.mage.map.detail.FeatureAction
import mil.nga.giat.mage.map.detail.FeatureDetails
import mil.nga.sf.Geometry

sealed class UserAction {
   class Email(val user: UserMapState): UserAction()
   class Phone(val user: UserMapState): UserAction()
   class Directions(val id: Long, val geometry: Geometry, val icon: Any?): UserAction()
   class Location(val geometry: Geometry): UserAction()
   class Details(val id: Long): UserAction()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationBottomSheet(
   state: UserMapState?,
   onDismiss: () -> Unit,
   onAction: ((Any) -> Unit)? = null,
   modifier: Modifier = Modifier
) {
   val bottomSheetState = rememberModalBottomSheetState()

   if (state != null) {
      ModalBottomSheet(
         onDismissRequest = { onDismiss() },
         sheetState = bottomSheetState
      ) {
         Surface(
            modifier = modifier
         ) {
            LocationDetails(
               userState = state
            )
         }
      }
   }
}

@Composable
private fun LocationDetails(
   userState: UserMapState?,
   onAction: ((Any) -> Unit)? = null
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
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
               Icon(
                  imageVector = Icons.Outlined.Email,
                  contentDescription = "Email"
               )
            }
         }
      }

      if (user.phone?.isNotEmpty() == true) {
         IconButton(
            modifier = Modifier.padding(end = 8.dp),
            onClick = { onPhone() }
         ) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
               Icon(
                  imageVector = Icons.Outlined.Phone,
                  contentDescription = "Phone"
               )
            }
         }
      }
   }
}