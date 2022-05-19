package mil.nga.giat.mage.observation.attachment

import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.google.accompanist.glide.rememberGlidePainter
import mil.nga.giat.mage.ui.theme.MageTheme
import mil.nga.giat.mage.ui.theme.topAppBarBackground

@Composable
fun AttachmentViewScreen(
   viewModel: AttachmentViewModel,
   onShare: () -> Unit,
   onClose: () -> Unit,
   onOpen: () -> Unit,
   onCancelDownload: () -> Unit
) {
   val attachmentState by viewModel.attachmentUri.observeAsState()
   val downloadProgress by viewModel.downloadProgress.observeAsState()

   MageTheme {
      Scaffold(
         topBar = {
            TopBar(
               onShare = onShare,
               onClose = onClose
            )
         },
         content = {
            attachmentState?.let { AttachmentContent(it, onOpen) }
         }
      )

      DownloadDialog(downloadProgress) {
         onCancelDownload()
      }
   }
}

@Composable
private fun DownloadDialog(
   progress: Float?,
   onCancel: () -> Unit
) {
   if (progress != null) {
      AlertDialog(
         onDismissRequest = {
            onCancel()
         },
         title = {
            Text(
               text = "Downloading Attachment",
               style = MaterialTheme.typography.h6,
            )
         },
         text = {
            Column(Modifier.padding(horizontal = 16.dp)) {
               LinearProgressIndicator(
                  progress = progress
               )
            }
         },
         buttons = {
            Row(
               horizontalArrangement = Arrangement.End,
               modifier = Modifier
                  .fillMaxWidth()
                  .padding(end = 16.dp)
            ) {
               TextButton(
                  onClick = { onCancel() }
               ) {
                  Text("Cancel")
               }
            }

         }
      )
   }
}

@Composable
private fun TopBar(
   onShare: () -> Unit,
   onClose: () -> Unit
) {
   TopAppBar(
      backgroundColor = MaterialTheme.colors.topAppBarBackground,
      contentColor = Color.White,
      title = { Text("Observation Attachment") },
      navigationIcon = {
         IconButton(onClick = { onClose.invoke() }) {
            Icon(Icons.Default.ArrowBack, "Cancel Edit")
         }
      },
      actions = {
         IconButton(
            onClick = onShare,
         ) {
            Icon(
               imageVector = Icons.Default.Share,
               contentDescription = "Share Attachment",
               tint = Color.White
            )
         }
      }
   )
}

@Composable
private fun AttachmentContent(
   state: AttachmentState,
   onOpen: () -> Unit
) {
   when (state) {
      is AttachmentState.ImageState -> AttachmentImageContent(state)
      is AttachmentState.MediaState -> AttachmentMediaContent(state)
      is AttachmentState.OtherState -> AttachmentOtherContent(state, onOpen)
   }
}

@Composable
private fun AttachmentImageContent(state: AttachmentState.ImageState) {
   val progress = CircularProgressDrawable(LocalContext.current)
   progress.strokeWidth = 10f
   progress.centerRadius = 80f
   progress.setColorSchemeColors(MaterialTheme.colors.primary.toArgb())
   progress.setTint(MaterialTheme.colors.primary.toArgb())
   progress.start()

   Box(
      Modifier
         .fillMaxWidth()
         .background(Color(0x19000000))
   ) {
      Image(
         painter = rememberGlidePainter(
            state.model,
            fadeIn = true,
            requestBuilder = {
               placeholder(progress)
            }
         ),
         contentDescription = "Image",
         Modifier.fillMaxSize()
      )
   }
}

@Composable
private fun AttachmentMediaContent(
   state: AttachmentState.MediaState,
) {
   val mediaController = MediaController(LocalContext.current)

   var loading by remember { mutableStateOf(true) }

   DisposableEffect(
      AndroidView(
         modifier = Modifier.fillMaxWidth(),
         factory = { context ->
            val videoView = VideoView(context).apply {
               layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            }

            mediaController.setAnchorView(videoView)
            mediaController.setMediaPlayer(videoView)

            videoView.setMediaController(mediaController)
            videoView.setVideoURI(state.uri)
            videoView.setOnPreparedListener {
               loading = false
               videoView.start()
               mediaController.show()
            }

            videoView
         }
      )
   ) {
      onDispose {
         mediaController.hide()
      }
   }

   if (loading) {
      Column(
         verticalArrangement = Arrangement.Center,
         horizontalAlignment = Alignment.CenterHorizontally,
         modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(MaterialTheme.colors.background)) {

         CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.disabled) {
            Text(
               text = "Buffering",
               style = MaterialTheme.typography.h6,
               modifier = Modifier.padding(bottom = 16.dp)
            )
         }

         CircularProgressIndicator()
      }
   }
}

@Composable
private fun AttachmentOtherContent(
   state: AttachmentState.OtherState,
   onOpen: () -> Unit
) {
   Column(
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
         .fillMaxSize()
         .padding(16.dp)
   ) {

      CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
         Icon(
            imageVector = Icons.Default.VisibilityOff,
            contentDescription = "Media Icon",
            tint = MaterialTheme.colors.primary.copy(LocalContentAlpha.current),
            modifier = Modifier
               .height(164.dp)
               .width(164.dp)
               .padding(bottom = 16.dp)
         )
      }

      CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
         Text(
            text = "Preview Not Available",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h5,
            modifier = Modifier.padding(bottom = 8.dp),
         )

         Text(
            text = "You may be able to view this content in another application",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.subtitle2,
            modifier = Modifier.padding(bottom = 32.dp)
         )
      }

      OutlinedButton(
         modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
         onClick = {
            onOpen.invoke()
         }
      ) {
         Text(text = "OPEN WITH")
      }
   }
}