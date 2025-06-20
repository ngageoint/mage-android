package mil.nga.giat.mage.ui.setup

import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SecurityUpdateWarning
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import mil.nga.giat.mage.R
import mil.nga.giat.mage.login.ServerUrlViewModel
import mil.nga.giat.mage.login.UrlState
import mil.nga.giat.mage.ui.theme.onSurfaceDisabled


data class HowToPageItem(
   val content: @Composable (scrollState: ScrollState) -> Unit
)

val HowToPagesList: List<HowToPageItem> = listOf(
   HowToPageItem(
      content = { scrollState -> MageInfo(scrollState) }
   ),
   HowToPageItem(
      content = { scrollState -> CreateServer(scrollState) }
   ),
   HowToPageItem(
      content = { scrollState -> CreateEvent(scrollState) }
   ),
   HowToPageItem(
      content = { scrollState -> CreateUser(scrollState) }
   ),
   HowToPageItem(
      content = { scrollState -> UserLogin(scrollState) }
   ),
   HowToPageItem(
      content = { scrollState -> RetrieveObservations(scrollState) }
   ),
   HowToPageItem(
      content = { scrollState -> CreateObservations(scrollState) }
   ),
   HowToPageItem(
      content = { scrollState -> SyncObservations(scrollState) }
   )
)

@Composable
fun TopLevelServerUrlScreen(
   onDone: () -> Unit,
   viewModel: ServerUrlViewModel = hiltViewModel()
) {
   val url = viewModel.url
   val urlState by viewModel.urlState.observeAsState()
   val unsavedData by viewModel.unsavedData.observeAsState(false)
   val onContinue = { viewModel.confirmUnsavedData() }
   val checkUrl: (String) -> Unit = { viewModel.checkUrl(it) }
   val appVersion = viewModel.version

   ServerUrlScreen(onDone, onContinue, checkUrl, url, urlState, unsavedData, appVersion)
}

@Composable
private fun ServerUrlScreen(onDone: () -> Unit, onContinue: () -> Unit, checkUrl: (String) -> Unit, urlStr: String, urlState: UrlState?, unsavedData: Boolean, appVersion: String?) {
   val scrollState = rememberScrollState()
   var errorState by remember { mutableStateOf<UrlState.Error?>(null) }
   val keyboardController = LocalSoftwareKeyboardController.current
   var url by remember { mutableStateOf(urlStr) }

   var showHowToPager by remember { mutableStateOf(false) }

   LaunchedEffect(urlState) {
      if (urlState is UrlState.Valid) {
         onDone()
      }
   }

   if (unsavedData) {
      UnsavedDataDialog(
         onDismiss = { onDone() },
         onContinue = { onContinue() }
      )
   }

   errorState?.let { state ->
      ErrorDialog(state) { errorState = null }
   }


   if (showHowToPager) {
      HowToPagerDialog(pages = HowToPagesList, onDismiss = {showHowToPager = false})
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
         Image(painter = painterResource(R.drawable.ic_wand_blue),
            contentDescription = "wand",
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

         Text(modifier = Modifier
            .align(alignment = Alignment.CenterHorizontally)
            .padding(top = 12.dp)
            .clickable { showHowToPager = true }, text = stringResource(R.string.how_to_questions), fontSize = 18.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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
                  checkUrl(url)
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
                  onClick = { onDone() },
                  modifier = Modifier.weight(1f)
               ) {
                  Text("Cancel")
               }
            }

            Button(
               onClick = {
                  keyboardController?.hide()
                  checkUrl(url)
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

         appVersion?.let{ version ->
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


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HowToPagerDialog(
   pages: List<HowToPageItem>,
   onDismiss: () -> Unit
) {
   val pagerState = rememberPagerState(pageCount = { pages.size })

   val scrollStates = remember { List(pages.size) { ScrollState(0) } }

   LaunchedEffect(pagerState.settledPage) {
      // When the settled page changes, scroll its corresponding ScrollState to the top
      // This ensures that when a user swipes to a new page and the swipe settles,
      // the new page's content is scrolled to the top.
      scrollStates[pagerState.settledPage].animateScrollTo(0)
   }

   Dialog(
      onDismissRequest = onDismiss,
      properties = DialogProperties(dismissOnClickOutside = true, usePlatformDefaultWidth = false)
   ) {
      Surface(
         shape = RoundedCornerShape(16.dp),
         color = colorResource(R.color.how_to_background),
         modifier = Modifier
            .fillMaxHeight(0.95f)
            .fillMaxWidth(0.9f)
            .defaultMinSize(minHeight = 300.dp)
      ) {
         Column(horizontalAlignment = Alignment.CenterHorizontally
         ) {
            HorizontalPager(
               state = pagerState,
               modifier = Modifier
                  .weight(1f)
                  .fillMaxWidth()
                  .padding(10.dp)
            ) { pageIndex ->
               HowToPagesList.get(pageIndex).content(scrollStates[pageIndex])
            }

            Spacer(modifier = Modifier.height(16.dp))

            //pager indicators
            Row(
               Modifier
                  .height(24.dp)
                  .fillMaxWidth(),
               horizontalArrangement = Arrangement.Center,
               verticalAlignment = Alignment.CenterVertically
            ) {
               repeat(pages.size) { iteration ->
                  val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else Color.White
                  Box(
                     modifier = Modifier
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(10.dp)
                  )
               }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
               onClick = onDismiss,
               modifier = Modifier
                  .fillMaxWidth()
                  .padding(15.dp)
            ) {
               Text("Close")
            }
         }
      }
   }
}

@Composable
private fun Incompatible(
   state: UrlState.Incompatible
) {
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

      AdminContact(
         text = "Your MAGE application is not compatible with server version ${state.version}.  Please update your application or contact your MAGE administrator for support.",
         contact = state.contact,
         style = MaterialTheme.typography.bodyLarge.copy(
            color = LocalContentColor.current.copy(alpha = .87f)
         ).toSpanStyle(),
         emailState = EmailState(
            subject = "MAGE Compatibility",
            body = "MAGE Application not compatible with server version ${state.version}"
         )
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

@Composable
fun MageInfo(scrollState: ScrollState) {
   HowToPageLayout(scrollState, R.string.how_to_info_header) { onImageClick ->
      Text(text = stringResource(R.string.how_to_info_body), color = Color.White, fontSize = 14.sp)

      Spacer(modifier = Modifier.height(20.dp))

      Image (
         painter = painterResource(id = R.drawable.how_to_native_observations),
         contentDescription = "Mage Server",
         modifier = Modifier
            .clickable { onImageClick(R.drawable.how_to_native_observations) }
            .clip(RoundedCornerShape(8.dp)),
         contentScale = ContentScale.Fit
      )
   }
}

@Composable
fun CreateServer(scrollState: ScrollState) {
   val uriHandler = LocalUriHandler.current
   val setupLink = stringResource(R.string.how_to_server_link)
   val annotatedLinkString = buildAnnotatedString {
      pushStringAnnotation(tag = "URL", annotation = setupLink)
      withStyle(
         style = SpanStyle(
            color = colorResource(R.color.light_blue),
            textDecoration = TextDecoration.Underline,
            fontSize = 18.sp
         )
      ) {
         append("MAGE Setup Guide")
      }
      pop()
   }

   HowToPageLayout(scrollState, R.string.how_to_server_header) { onImageClick ->
      Text(text = stringResource(R.string.how_to_server_body), color = Color.White, fontSize = 14.sp)

      Spacer(modifier = Modifier.height(20.dp))

      ClickableText(
         text = annotatedLinkString,
         style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
         onClick = { offset ->
            annotatedLinkString
               .getStringAnnotations(tag = "URL", start = offset, end = offset)
               .firstOrNull()?.let { annotation ->
                  uriHandler.openUri(annotation.item)
               }
         },
         modifier = Modifier.padding(bottom = 16.dp)
      )

      Spacer(modifier = Modifier.height(20.dp))

   }
}


@Composable
fun CreateEvent(scrollState: ScrollState) {
   HowToPageLayout(scrollState, R.string.how_to_event_header) { onImageClick ->
      Text(text = stringResource(R.string.how_to_event_body), color = Color.White, fontSize = 14.sp)

      Spacer(modifier = Modifier.height(20.dp))

      Image (
         painter = painterResource(id = R.drawable.how_to_new_event),
         contentDescription = "Mage New Event",
         modifier = Modifier
            .fillMaxWidth()
            .clickable { onImageClick(R.drawable.how_to_new_event) }
            .clip(RoundedCornerShape(8.dp)),
         contentScale = ContentScale.Fit
      )

      Spacer(modifier = Modifier.height(20.dp))

      Image (
         painter = painterResource(id = R.drawable.how_to_event_add),
         contentDescription = "Mage Add User",
         modifier = Modifier
            .fillMaxWidth()
            .clickable { onImageClick(R.drawable.how_to_event_add) }
            .clip(RoundedCornerShape(8.dp)),
         contentScale = ContentScale.Fit
      )
   }
}

@Composable
fun CreateUser(scrollState: ScrollState) {
   HowToPageLayout(scrollState, R.string.how_to_create_header) { onImageClick ->
      Text(text = stringResource(R.string.how_to_create_body), color = Color.White, fontSize = 14.sp)

      Spacer(modifier = Modifier.height(20.dp))

      Image (
         painter = painterResource(id = R.drawable.how_to_create),
         contentDescription = "Mage Add User",
         modifier = Modifier
            .clickable { onImageClick(R.drawable.how_to_create) }
            .clip(RoundedCornerShape(8.dp)),
         contentScale = ContentScale.Fit
      )
   }
}

@Composable
fun UserLogin(scrollState: ScrollState) {
   HowToPageLayout(scrollState, R.string.how_to_login_header) { onImageClick ->
      Text(text = stringResource(R.string.how_to_login_body), color = Color.White, fontSize = 14.sp)

      Spacer(modifier = Modifier.height(20.dp))

      Image (
         painter = painterResource(id = R.drawable.how_to_sign_in),
         contentDescription = "Mage Users List",
         modifier = Modifier
            .clickable { onImageClick(R.drawable.how_to_sign_in) }
            .clip(RoundedCornerShape(8.dp)),
         contentScale = ContentScale.Fit
      )

      Spacer(modifier = Modifier.height(20.dp))

      Text(text = stringResource(R.string.how_to_login_body_two), color = Color.White, fontSize = 14.sp)

      Spacer(modifier = Modifier.height(20.dp))

      Image (
         painter = painterResource(id = R.drawable.how_to_select_event),
         contentDescription = "Mage Add User",
         modifier = Modifier
            .clickable { onImageClick(R.drawable.how_to_select_event) }
            .clip(RoundedCornerShape(8.dp)),
         contentScale = ContentScale.Fit
      )
   }
}

@Composable
fun RetrieveObservations(scrollState: ScrollState) {
   HowToPageLayout(scrollState, R.string.how_to_observations_header) { onImageClick ->
      Text(text = stringResource(R.string.how_to_observations_body), color = Color.White, fontSize = 14.sp)

      Spacer(modifier = Modifier.height(20.dp))

      Image (
         painter = painterResource(id = R.drawable.how_to_observations),
         contentDescription = "Mage Users List",
         modifier = Modifier
            .clickable { onImageClick(R.drawable.how_to_observations) }
            .clip(RoundedCornerShape(8.dp)),
         contentScale = ContentScale.Fit
      )

      Spacer(modifier = Modifier.height(20.dp))

      Text(text = stringResource(R.string.how_to_observations_body_two), color = Color.White, fontSize = 14.sp)

      Spacer(modifier = Modifier.height(20.dp))

      Image (
         painter = painterResource(id = R.drawable.how_to_time_filter),
         contentDescription = "Mage Users List",
         modifier = Modifier
            .clickable { onImageClick(R.drawable.how_to_time_filter) }
            .clip(RoundedCornerShape(8.dp)),
         contentScale = ContentScale.Fit
      )

      Spacer(modifier = Modifier.height(20.dp))

      Text(text = stringResource(R.string.how_to_observations_body_three), color = Color.White, fontSize = 14.sp)

      Spacer(modifier = Modifier.height(20.dp))

      Image (
         painter = painterResource(id = R.drawable.how_to_fetching),
         contentDescription = "Mage Add User",
         modifier = Modifier
            .clickable { onImageClick(R.drawable.how_to_fetching) }
            .clip(RoundedCornerShape(8.dp)),
         contentScale = ContentScale.Fit
      )
   }
}


@Composable
fun CreateObservations(scrollState: ScrollState) {
   HowToPageLayout(scrollState, R.string.how_to_create_observation_header) { onImageClick ->
      Text(text = stringResource(R.string.how_to_create_observation_body), color = Color.White, fontSize = 14.sp)

      Spacer(modifier = Modifier.height(20.dp))

      Image (
         painter = painterResource(id = R.drawable.how_to_point),
         contentDescription = "Mage Users List",
         modifier = Modifier
            .clickable { onImageClick(R.drawable.how_to_point) }
            .clip(RoundedCornerShape(8.dp)),
         contentScale = ContentScale.Fit
      )

      Spacer(modifier = Modifier.height(20.dp))

      Text(text = stringResource(R.string.how_to_create_observation_body_two), color = Color.White, fontSize = 14.sp)

      Spacer(modifier = Modifier.height(20.dp))

      Image (
         painter = painterResource(id = R.drawable.how_to_polygon),
         contentDescription = "Mage Users List",
         modifier = Modifier
            .clickable { onImageClick(R.drawable.how_to_polygon) }
            .clip(RoundedCornerShape(8.dp)),
         contentScale = ContentScale.Fit
      )
   }
}


@Composable
fun SyncObservations(scrollState: ScrollState) {
   HowToPageLayout(scrollState, R.string.how_to_sync_observation_header) { onImageClick ->
      Text(text = stringResource(R.string.how_to_sync_observation_body), color = Color.White, fontSize = 14.sp)

      Spacer(modifier = Modifier.height(20.dp))

      Image (
         painter = painterResource(id = R.drawable.how_to_sync_settings),
         contentDescription = "Mage Users List",
         modifier = Modifier
            .clickable { onImageClick(R.drawable.how_to_sync_settings) }
            .clip(RoundedCornerShape(8.dp)),
         contentScale = ContentScale.Fit
      )
   }
}

@Composable
fun HowToPageLayout(scrollState: ScrollState, titleResId: Int,
                    bodyContent: @Composable (onImageClick: (imageId: Int) -> Unit) -> Unit) {

   var showImage by remember { mutableStateOf(false) }
   var selectedImageId by remember { mutableIntStateOf(0) }

   Column(modifier = Modifier
      .fillMaxSize()
      .padding(start = 6.dp, end = 6.dp, top = 16.dp)
      .verticalScroll(scrollState),
      horizontalAlignment = Alignment.CenterHorizontally) {

      Text(text = stringResource(titleResId), color = Color.White, fontSize = 18.sp, modifier = Modifier.padding(bottom = 5.dp))

      Divider(color = Color.White, thickness = 1.dp, modifier = Modifier.padding(bottom = 20.dp))

      bodyContent { imageResIdToShow ->
         selectedImageId = imageResIdToShow
         showImage = true
      }

   }

   if (showImage) {
      ZoomableImageDialog(
         imageResId = selectedImageId,
         onDismiss = { showImage = false }
      )
   }
}


@Composable
fun ZoomableImageDialog(
   imageResId: Int, onDismiss: () -> Unit
) {
   // State for zoom and offset (pan)
   var scale by remember { mutableFloatStateOf(1f) }
   var offsetX by remember { mutableFloatStateOf(0f) }
   var offsetY by remember { mutableFloatStateOf(0f) }

   Dialog(
      onDismissRequest = onDismiss,
      properties = DialogProperties(usePlatformDefaultWidth = false)
   ) {
      Surface(
         modifier = Modifier.fillMaxSize(),
         color = Color.Transparent
      ) {
         Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Image(
               painter = painterResource(id = imageResId),
               contentDescription = "Mage Server",
               modifier = Modifier
                  .fillMaxSize(.8f)
                  .padding(16.dp)
                  .pointerInput(Unit) {
                     detectTransformGestures { centroid, pan, zoom, _ ->
                        scale =
                           (scale * zoom).coerceIn(1f, 5f) // Apply zoom, coerce to min/max scale

                        // Calculate new offsets considering the current scale
                        // This logic helps keep the image centered around the pinch point
                        // and allows panning.
                        val newOffsetX = offsetX + pan.x * scale
                        val newOffsetY = offsetY + pan.y * scale

                        // Basic boundary checks (can be improved for more precise edge locking)
                        // These are simplified; for perfect edge locking, you'd need to consider image dimensions
                        // after scaling and the screen dimensions.
                        val maxOffsetX = (scale - 1) * size.width / 2
                        val maxOffsetY = (scale - 1) * size.height / 2

                        offsetX = newOffsetX.coerceIn(-maxOffsetX, maxOffsetX)
                        offsetY = newOffsetY.coerceIn(-maxOffsetY, maxOffsetY)

                        // If scale is reset to 1f, also reset offsets
                        if (scale == 1f) {
                           offsetX = 0f
                           offsetY = 0f
                        }
                     }
                  }
                  .graphicsLayer(
                     scaleX = scale,
                     scaleY = scale,
                     translationX = offsetX,
                     translationY = offsetY
                  ),
               contentScale = ContentScale.Fit
            )
            IconButton(
               onClick = onDismiss,
               modifier = Modifier
                  .align(Alignment.TopEnd)
                  .padding(8.dp)
            ) {
               Icon(
                  imageVector = Icons.Filled.Close,
                  contentDescription = "Close image",
                  tint = Color.White
               )
            }
         }
      }
   }
}


@Preview
@Composable
fun PreviewServerUrlScreen() {
   ServerUrlScreen({}, {}, {}, "", null, false, null)

}

