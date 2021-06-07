package mil.nga.giat.mage.form.view

import android.webkit.MimeTypeMap
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.google.accompanist.glide.rememberGlidePainter
import mil.nga.giat.mage.glide.transform.VideoOverlayTransformation
import mil.nga.giat.mage.sdk.datastore.observation.Attachment
import java.util.*

@Composable
fun AttachmentsViewContent(
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
          AttachmentViewContent(attachment) { onClick?.invoke(attachment) }
        }
      }
    }
  }
}

@Composable
fun AttachmentViewContent(
  attachment: Attachment,
  onClick: (() -> Unit)? = null
) {
  val isVideo = when {
    attachment.localPath != null -> {
      val fileExtension = MimeTypeMap.getFileExtensionFromUrl(attachment.localPath)
      val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase(
        Locale.ROOT))
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
      .width(100.dp)
      .height(100.dp)
      .padding(8.dp)
      .clip(MaterialTheme.shapes.medium)
      .clickable { onClick?.invoke() }) {
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
  }
}