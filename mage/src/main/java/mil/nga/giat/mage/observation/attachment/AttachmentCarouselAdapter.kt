package mil.nga.giat.mage.observation.attachment

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import mil.nga.giat.mage.R
import mil.nga.giat.mage.database.model.observation.Attachment
import mil.nga.giat.mage.database.model.observation.AttachmentProcessingState
import mil.nga.giat.mage.database.model.observation.hasLocalFile
import mil.nga.giat.mage.database.model.observation.processingState
import mil.nga.giat.mage.glide.GlideApp
import mil.nga.giat.mage.glide.transform.VideoOverlayTransformation

class AttachmentCarouselAdapter(
   private val context: Context,
   private val onAttachmentClick: (Attachment) -> Unit
) : RecyclerView.Adapter<AttachmentCarouselAdapter.ViewHolder>() {

   private var attachments: List<Attachment> = emptyList()

   // A fresh Drawable instance per bind - a single shared instance can't be the foreground of
   // more than one visible view at once, since Drawable state (bounds, callback) isn't shareable.
   private val selectableItemBackgroundResId: Int by lazy {
      val outValue = TypedValue()
      context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
      outValue.resourceId
   }

   fun submitAttachments(attachments: Collection<Attachment>) {
      // Passed (and in-flight) attachments lead the carousel so a mixed pass/fail observation's
      // default (page 0) slide is always a real image, not whichever attachment happened to
      // upload first - failed ones only surface first if every attachment failed.
      this.attachments = attachments.sortedBy { it.processingState == AttachmentProcessingState.FAILED }
      notifyDataSetChanged()
   }

   class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
      val imageView: ImageView = view.findViewById(R.id.attachment_image)
      val placeholderIcon: ImageView = view.findViewById(R.id.attachment_placeholder_icon)
      val title: TextView = view.findViewById(R.id.attachment_title)
      val label: TextView = view.findViewById(R.id.attachment_label)
   }

   override fun getItemCount() = attachments.size

   override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
      val view = LayoutInflater.from(parent.context).inflate(R.layout.attachment_carousel_page, parent, false)
      return ViewHolder(view)
   }

   override fun onBindViewHolder(holder: ViewHolder, position: Int) {
      val attachment = attachments[position]
      val isUploading = attachment.processingState == AttachmentProcessingState.UPLOADING
      val isPending = attachment.processingState == AttachmentProcessingState.PENDING
      val isFailed = attachment.processingState == AttachmentProcessingState.FAILED

      holder.imageView.setImageDrawable(null)

      if (isFailed || ((isUploading || isPending) && !attachment.hasLocalFile)) {
         holder.placeholderIcon.visibility = View.VISIBLE
         holder.itemView.isClickable = false
         holder.itemView.isFocusable = false
         holder.itemView.foreground = null
         holder.itemView.setOnTouchListener(null)
         holder.itemView.setOnClickListener(null)
         holder.placeholderIcon.setImageResource(
            if (isFailed) R.drawable.ic_error_outline_24dp else R.drawable.ic_cloud_upload_24dp
         )
         holder.title.text = if (isFailed) "Upload Failed" else "Upload Pending"
         holder.title.visibility = View.VISIBLE
         holder.label.text = attachment.name
         holder.label.visibility = if (attachment.name.isNullOrBlank()) View.GONE else View.VISIBLE
         return
      }

      holder.placeholderIcon.visibility = View.GONE
      holder.title.visibility = View.GONE
      holder.label.visibility = View.GONE
      holder.imageView.scaleType = ImageView.ScaleType.CENTER_CROP
      ImageViewCompat.setImageTintList(holder.imageView, null)

      var isVideo = false
      if (attachment.localPath != null) {
         val fileExtension = MimeTypeMap.getFileExtensionFromUrl(attachment.localPath)
         val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.lowercase())
         isVideo = mimeType?.startsWith("video/") == true
      } else if (attachment.contentType != null) {
         isVideo = attachment.contentType.startsWith("video/")
      }

      val transformations = mutableListOf<BitmapTransformation>(CenterCrop())
      if (isVideo) {
         transformations.add(VideoOverlayTransformation(context))
      }

      GlideApp.with(context)
         .asBitmap()
         .load(attachment)
         .fallback(R.drawable.ic_attachment_200dp)
         .error(R.drawable.ic_attachment_200dp)
         .transforms(*transformations.toTypedArray())
         .into(holder.imageView)

      holder.itemView.isClickable = true
      holder.itemView.isFocusable = true
      holder.itemView.foreground = ContextCompat.getDrawable(context, selectableItemBackgroundResId)
      holder.itemView.setOnTouchListener(null)
      holder.itemView.setOnClickListener { onAttachmentClick(attachment) }
   }
}
