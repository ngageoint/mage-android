package mil.nga.giat.mage.ui.map.search

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import mil.nga.giat.mage.search.SearchResponse
import mil.nga.giat.mage.ui.coordinate.CoordinateText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchButton(
   expanded: Boolean,
   response: SearchResponse? = null,
   onExpand: () -> Unit,
   onTextChanged: (String) -> Unit,
   onLocationTap: (LatLng) -> Unit,
   onLocationCopy: (String) -> Unit
) {
   val focusRequester = remember { FocusRequester() }
   val configuration = LocalConfiguration.current
   val interactionSource = remember { MutableInteractionSource() }
   var text by remember { mutableStateOf("") }
   val width = if (expanded) configuration.screenWidthDp.dp.minus(88.dp) else 40.dp

   LaunchedEffect(expanded) {
      if (expanded) {
         focusRequester.requestFocus()
      }
   }

   Surface(
      tonalElevation = 6.dp,
      shadowElevation = 6.dp,
      shape = FloatingActionButtonDefaults.smallShape,
      color = MaterialTheme.colorScheme.primaryContainer,
      contentColor = MaterialTheme.colorScheme.primaryContainer,
      modifier = Modifier.padding(4.dp)
   ) {
      Column {
         BasicTextField(
            value = text,
            onValueChange = {
               text = it
               onTextChanged(it)
            },
            textStyle = TextStyle(
               color = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            interactionSource = interactionSource,
            enabled = expanded,
            singleLine = true,
            modifier = Modifier
               .animateContentSize()
               .height(40.dp)
               .width(width)
               .focusRequester(focusRequester)
         ) {
            TextFieldDefaults.DecorationBox(
               value = text,
               innerTextField = it,
               enabled = expanded,
               singleLine = true,
               visualTransformation = VisualTransformation.None,
               interactionSource = interactionSource,
               placeholder = {
                  Text(text = "Search")
               },
               leadingIcon = {
                  IconButton(onClick = { onExpand() }) {
                     Icon(
                        imageVector = Icons.Default.Search,
                        tint = MaterialTheme.colorScheme.tertiary,
                        contentDescription = "Search"
                     )
                  }
               },
               trailingIcon = {
                  IconButton(
                     onClick = {
                        text = ""
                        onTextChanged("")
                     }
                  ) {
                     Icon(
                        imageVector = Icons.Default.Close,
                        modifier = Modifier.size(18.dp),
                        contentDescription = "Search Clear"
                     )
                  }
               },
               colors = TextFieldDefaults.colors(
                  focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                  unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                  unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                  disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
                  focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                  focusedIndicatorColor = MaterialTheme.colorScheme.primaryContainer,
                  unfocusedIndicatorColor = MaterialTheme.colorScheme.primaryContainer,
                  disabledIndicatorColor = MaterialTheme.colorScheme.primaryContainer
               ),
               contentPadding = TextFieldDefaults.contentPaddingWithoutLabel(
                  top = 0.dp,
                  bottom = 0.dp,
               )
            )
         }

         if (response != null && response is SearchResponse.Success) {
            val scrollState = rememberScrollState()
            val searchHeight = configuration.screenHeightDp.dp.div(3)

            Column(
               modifier = Modifier
                  .width(width)
                  .heightIn(0.dp, searchHeight)
                  .verticalScroll(scrollState)
            ) {
               response.results.forEach { result ->
                  HorizontalDivider(Modifier.padding(horizontal = 8.dp))

                  Row(
                     verticalAlignment = Alignment.CenterVertically,
                     modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                  ) {
                     Column(Modifier.weight(1f)) {
                        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                           Text(
                              text = result.name,
                              style = MaterialTheme.typography.titleMedium,
                              fontWeight = FontWeight.Medium
                           )
                        }

                        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                           Text(
                              text = result.address ?: "",
                              style = MaterialTheme.typography.titleSmall
                           )
                        }

                        CoordinateText(
                           latLng = LatLng(result.location.latitude, result.location.longitude),
                           onCopiedToClipboard = { onLocationCopy(it) }
                        )
                     }

                     result.location.let {
                        IconButton(
                           onClick = { onLocationTap(it) }
                        ) {
                           Icon(
                              imageVector = Icons.Default.LocationSearching,
                              tint = MaterialTheme.colorScheme.tertiary,
                              contentDescription = "Zoom To Search Result"
                           )
                        }
                     }
                  }
               }
            }
         }
      }
   }
}