package mil.nga.giat.mage.compat.server5.form.view

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.webkit.MimeTypeMap
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import mil.nga.giat.mage.glide.transform.VideoOverlayTransformation
import mil.nga.giat.mage.observation.edit.AttachmentAction
import mil.nga.giat.mage.database.model.observation.Attachment
import java.util.*

@Composable
fun AttachmentsViewContentServer5(
   attachments: Collection<Attachment>,
   onClick: ((Attachment) -> Unit)? = null
) {
   if (attachments.isNotEmpty()) {
      Row(
         verticalAlignment = Alignment.CenterVertically,
         modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 8.dp, end = 8.dp)
      ) {
         CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Text(
               text = "ATTACHMENTS",
               style = MaterialTheme.typography.caption,
               fontWeight = FontWeight.SemiBold,
               modifier = Modifier
                  .weight(1f)
                  .padding(vertical = 8.dp)
            )
         }
      }

      Card(
         Modifier.fillMaxWidth()
      ) {
         Row(
            Modifier
               .fillMaxWidth()
               .horizontalScroll(rememberScrollState())) {
            for (attachment in attachments) {
               AttachmentViewContent_server5(attachment, deletable = false) { onClick?.invoke(attachment) }
            }
         }
      }
   }
}

@Composable
fun AttachmentViewContent_server5(
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

   val transformations: MutableList<BitmapTransformation> = mutableListOf(CenterCrop())
   if (isVideo) {
      transformations.add(VideoOverlayTransformation(LocalContext.current))
   }

   var bitmap by remember { mutableStateOf<Bitmap?>(null)}

   Glide.with(LocalContext.current)
      .asBitmap()
      .load(attachment)
      .transform(*transformations.toTypedArray())
      .into(object : CustomTarget<Bitmap>(100, 100) {
         override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
            bitmap = resource
         }

         override fun onLoadCleared(placeholder: Drawable?) {}
      })

   Box(
      Modifier
         .width(100.dp)
         .height(100.dp)
         .padding(vertical = 8.dp, horizontal = 4.dp)
         .clip(MaterialTheme.shapes.large)
         .clickable { onAttachmentAction?.invoke(AttachmentAction.VIEW) }) {

      bitmap?.let { image ->
         Image(
            bitmap = image.asImageBitmap(),
            contentDescription = "Attachment Preview",
            modifier = Modifier.fillMaxSize()
         )
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