package mil.nga.giat.mage.observation.attachment

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import mil.nga.giat.mage.R
import mil.nga.giat.mage.database.model.observation.Attachment
import mil.nga.giat.mage.glide.GlideApp
import mil.nga.giat.mage.glide.transform.VideoOverlayTransformation

class AttachmentCarouselAdapter(
   private val context: Context,
   private val onAttachmentClick: (Attachment) -> Unit
) : RecyclerView.Adapter<AttachmentCarouselAdapter.ViewHolder>() {

   private var attachments: List<Attachment> = emptyList()

   /** Invoked when the user taps a failed attachment's thumbnail — there's nothing to view, so this opens the observation instead. */
   var onFailedAttachmentClick: (() -> Unit)? = null

   fun submitAttachments(attachments: Collection<Attachment>) {
      this.attachments = attachments.toList()
      notifyDataSetChanged()
   }

   class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
      val imageView: ImageView = view.findViewById(R.id.attachment_image)
      val placeholderIcon: ImageView = view.findViewById(R.id.attachment_placeholder_icon)
      val label: TextView = view.findViewById(R.id.attachment_label)
   }

   override fun getItemCount() = attachments.size

   override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
      val view = LayoutInflater.from(parent.context).inflate(R.layout.attachment_carousel_page, parent, false)
      return ViewHolder(view)
   }

   override fun onBindViewHolder(holder: ViewHolder, position: Int) {
      val attachment = attachments[position]
      val isUploading = attachment.isDirty && attachment.processingStatus == null
      val isPending = attachment.processingStatus == "pending"
      val isFailed = attachment.processingStatus == "rejected" || attachment.processingStatus == "error"

      holder.imageView.setImageDrawable(null)

      if (isUploading || isPending || isFailed) {
         holder.placeholderIcon.visibility = View.VISIBLE
         if (isFailed) {
            holder.placeholderIcon.setImageResource(R.drawable.ic_error_24dp)
            ImageViewCompat.setImageTintList(
               holder.placeholderIcon,
               ColorStateList.valueOf(ContextCompat.getColor(context, R.color.text_primary))
            )
            holder.label.visibility = View.GONE
            holder.itemView.setOnClickListener { onFailedAttachmentClick?.invoke() }
         } else {
            ImageViewCompat.setImageTintList(holder.placeholderIcon, null)
            val progress = CircularProgressDrawable(context)
            progress.setStrokeWidth(8f)
            progress.centerRadius = 28f
            progress.setColorSchemeColors(
               ContextCompat.getColor(context, R.color.md_blue_600),
               ContextCompat.getColor(context, R.color.md_orange_A200)
            )
            progress.start()
            holder.placeholderIcon.setImageDrawable(progress)
            holder.label.text = if (isPending) "Upload pending..." else "Uploading..."
            holder.label.visibility = View.VISIBLE
            holder.itemView.setOnClickListener(null)
         }
         return
      }

      holder.placeholderIcon.visibility = View.GONE
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

      holder.itemView.setOnClickListener { onAttachmentClick(attachment) }
   }
}
