package mil.nga.giat.mage.form.view

import android.webkit.MimeTypeMap
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import mil.nga.giat.mage.R
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
   val isUploading = attachment.isDirty && attachment.processingStatus == null
   val isPending = attachment.processingStatus == "pending"
   val isFailed = attachment.processingStatus == "rejected" || attachment.processingStatus == "error"
   var messageExpanded by remember(attachment) { mutableStateOf(false) }

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
         .clickable {
            if (isFailed) {
               messageExpanded = !messageExpanded
            } else {
               onAttachmentAction?.invoke(AttachmentAction.VIEW)
            }
         }) {
      if (isUploading || isPending || isFailed) {
         Column(
            modifier = Modifier
               .fillMaxSize()
               .background(colorResource(R.color.background_attachment)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
         ) {
            if (!(isFailed && messageExpanded)) {
               if (isFailed) {
                  Icon(
                     Icons.Outlined.ErrorOutline,
                     contentDescription = "Upload failed",
                     tint = MaterialTheme.colors.onSurface,
                     modifier = Modifier.size(80.dp)
                  )
               } else {
                  CircularProgressIndicator(modifier = Modifier.size(32.dp))
               }
               Spacer(modifier = Modifier.height(8.dp))
            }
            if (isFailed) {
               Text(
                  text = "Upload Failed",
                  style = MaterialTheme.typography.subtitle1,
                  fontWeight = FontWeight.Bold,
                  textAlign = TextAlign.Center
               )
               if (!attachment.name.isNullOrBlank()) {
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                     text = attachment.name,
                     style = MaterialTheme.typography.caption,
                     textAlign = TextAlign.Center,
                     maxLines = 1,
                     overflow = TextOverflow.Ellipsis,
                     modifier = Modifier.padding(horizontal = 16.dp)
                  )
               }
               Text(
                  text = if (messageExpanded) (attachment.processingMessage ?: "Upload failed") else "Tap for Details",
                  style = MaterialTheme.typography.caption,
                  textAlign = TextAlign.Center,
                  maxLines = if (messageExpanded) Int.MAX_VALUE else 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.animateContentSize()
               )
            } else {
               Text(
                  text = if (isPending) "Upload pending..." else "Uploading...",
                  style = MaterialTheme.typography.overline,
                  textAlign = TextAlign.Center
               )
            }
         }
      } else {
         @OptIn(ExperimentalGlideComposeApi::class)
         GlideImage(
            model = attachment,
            contentDescription = "Attachment Preview",
            modifier = Modifier.fillMaxSize(),
         ) { it.transform(*transformations.toTypedArray()) }
      }

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