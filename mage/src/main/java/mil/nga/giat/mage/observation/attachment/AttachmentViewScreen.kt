package mil.nga.giat.mage.observation.attachment

import android.content.res.ColorStateList
import android.net.Uri
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
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LiveData
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.google.accompanist.glide.rememberGlidePainter
import mil.nga.giat.mage.R
import mil.nga.giat.mage.ui.theme.MageTheme
import mil.nga.giat.mage.ui.theme.topAppBarBackground

@Composable
fun AttachmentViewScreen(
   liveData: LiveData<AttachmentState>,
   onClose: (() -> Unit)? = null
) {
   MageTheme {
      Scaffold(
         topBar = {
            TopBar { onClose?.invoke() }
         },
         content = {
            val state by liveData.observeAsState()
            state?.let { AttachmentContent(it) }
         }
      )
   }
}

@Composable
private fun TopBar(
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
      }
   )
}


@Composable
private fun AttachmentContent(state: AttachmentState) {
   when {
      state.contentType.startsWith("image") -> AttachmentImageContent(state)
      state.contentType.startsWith("video") -> AttachmentMediaContent(state)
      state.contentType.startsWith("audio") -> AttachmentMediaContent(state)
   }
}

@Composable
private fun AttachmentImageContent(state: AttachmentState) {
   val request: Any = when(state) {
      is AttachmentFile -> state.uri
      is AttachmentModel -> state.attachment
      is AttachmentUrl -> state.uri
   }

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
            request,
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
   state: AttachmentState,
) {
   val uri = when(state) {
      is AttachmentFile -> state.uri
      is AttachmentUrl -> state.uri
      is AttachmentModel -> Uri.parse(state.attachment.url)
   }

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
            videoView.setVideoURI(uri)
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