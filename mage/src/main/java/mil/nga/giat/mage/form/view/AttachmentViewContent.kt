package mil.nga.giat.mage.form.view

import android.webkit.MimeTypeMap
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.google.accompanist.glide.rememberGlidePainter
import mil.nga.giat.mage.form.field.Media
import mil.nga.giat.mage.glide.transform.VideoOverlayTransformation
import mil.nga.giat.mage.observation.edit.AttachmentAction
import mil.nga.giat.mage.sdk.datastore.observation.Attachment
import java.util.*

@Composable
fun AttachmentsViewContent(
   media: List<Media>,
   deletable: Boolean = false,
   onAttachmentAction: ((AttachmentAction, Media) -> Unit)? = null,
) {
   if (media.isNotEmpty()) {
      Column(Modifier.fillMaxWidth()) {
         val oddMedia = if (media.size % 2 == 0) null else media[0]
         val evenMedia = if (media.size % 2 == 0) media else media.drop(1)

         if (oddMedia != null) {
            AttachmentViewContent(oddMedia, deletable) { action ->
               onAttachmentAction?.invoke(action, oddMedia)
            }
         }

         evenMedia.chunked(2).forEach {  (media1, media2) ->
            Row {
               Column(
                  Modifier
                     .weight(1f)
                     .padding(top = 4.dp, end = 2.dp)
               ) {
                  AttachmentViewContent(media1, deletable) { action ->
                     onAttachmentAction?.invoke(action, media1)
                  }
               }
               Column(
                  Modifier
                     .weight(1f)
                     .padding(top = 4.dp, start = 2.dp)
               ) {
                  AttachmentViewContent(media2, deletable) { action ->
                     onAttachmentAction?.invoke(action, media2)
                  }
               }
            }
         }
      }
   }
}

@Composable
fun AttachmentViewContent(
   media: Media,
   deletable: Boolean,
   onAttachmentAction: ((AttachmentAction) -> Unit)? = null
) {
   val isVideo = when {
      media.localPath != null -> {
         val fileExtension = MimeTypeMap.getFileExtensionFromUrl(media.localPath)
         val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.lowercase(Locale.ROOT))
         mimeType?.startsWith("video/") == true
      }
      media.contentType != null -> {
         media.contentType.startsWith("video/")
      }
      else -> false
   }

   val transformations: MutableList<BitmapTransformation> = mutableListOf()
   transformations.add(CenterCrop())
   if (isVideo) {
      transformations.add(VideoOverlayTransformation(LocalContext.current))
   }

   Box(
      Modifier
         .fillMaxWidth()
         .height(200.dp)
         .clip(MaterialTheme.shapes.large)
         .clickable { onAttachmentAction?.invoke(AttachmentAction.VIEW) }) {
      Image(
         painter = rememberGlidePainter(
            media,
            fadeIn = true,
            requestBuilder = {
               transforms(*transformations.toTypedArray())
            }
         ),
         contentDescription = "Attachment Preview",
         Modifier.fillMaxSize()
      )

      if (deletable) {
         FloatingActionButton(
            backgroundColor = MaterialTheme.colors.error,
            modifier = Modifier
               .align(Alignment.BottomEnd)
               .padding(16.dp)
               .defaultMinSize(minWidth = 40.dp, minHeight = 40.dp),
            onClick = { onAttachmentAction?.invoke(AttachmentAction.DELETE) }
         ) {
            Icon(
               Icons.Filled.Delete,
               tint = Color.White,
               contentDescription = "Delete attachment"
            )
         }
      }
   }
}