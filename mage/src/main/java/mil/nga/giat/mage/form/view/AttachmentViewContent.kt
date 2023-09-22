package mil.nga.giat.mage.form.view

import android.webkit.MimeTypeMap
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.google.accompanist.glide.rememberGlidePainter
import mil.nga.giat.mage.glide.transform.VideoOverlayTransformation
import mil.nga.giat.mage.observation.edit.AttachmentAction
import mil.nga.giat.mage.database.model.observation.Attachment
import java.util.*

@Composable
fun AttachmentsViewContent(
   attachments: List<Attachment>,
   deletable: Boolean = false,
   onAttachmentAction: ((AttachmentAction, Attachment) -> Unit)? = null,
) {
   Column(Modifier.fillMaxWidth()) {
      val oddAttachments = if (attachments.size % 2 == 0) null else attachments[0]
      val evenAttachment = if (attachments.size % 2 == 0) attachments else attachments.drop(1)

      if (oddAttachments != null) {
         AttachmentViewContent(oddAttachments, deletable) { action ->
            onAttachmentAction?.invoke(action, oddAttachments)
         }
      }

      evenAttachment.chunked(2).forEach {  (attachment1, attachment2) ->
         Row {
            Column(
               Modifier
                  .weight(1f)
                  .padding(top = 4.dp, end = 2.dp)
            ) {
               AttachmentViewContent(attachment1, deletable) { action ->
                  onAttachmentAction?.invoke(action, attachment1)
               }
            }
            Column(
               Modifier
                  .weight(1f)
                  .padding(top = 4.dp, start = 2.dp)
            ) {
               AttachmentViewContent(attachment2, deletable) { action ->
                  onAttachmentAction?.invoke(action, attachment2)
               }
            }
         }
      }
   }
}

@Composable
fun AttachmentViewContent(
   attachment: Attachment,
   deletable: Boolean,
   onAttachmentAction: ((AttachmentAction) -> Unit)? = null
) {
   val isVideo = when {
      attachment.localPath != null -> {
         val fileExtension = MimeTypeMap.getFileExtensionFromUrl(attachment.localPath)
         val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.lowercase(Locale.ROOT))
         mimeType?.startsWith("video/") == true
      }
      attachment.contentType != null -> {
         attachment.contentType.startsWith("video/")
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
            attachment,
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