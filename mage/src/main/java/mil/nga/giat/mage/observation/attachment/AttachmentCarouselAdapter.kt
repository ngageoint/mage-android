package mil.nga.giat.mage.observation.attachment

import android.content.Context
import android.content.res.ColorStateList
import android.text.TextUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
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

   // A fresh Drawable instance per bind - a single shared instance can't be the foreground of
   // more than one visible view at once, since Drawable state (bounds, callback) isn't shareable.
   private val selectableItemBackgroundResId: Int by lazy {
      val outValue = TypedValue()
      context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
      outValue.resourceId
   }

   // Invoked when a failed attachment's thumbnail is tapped, with the tapped view and the tap's
   // coordinates within it - there's nothing to view, so this opens the observation instead, and
   // plays the ripple on the card underneath (the same one a direct tap on the card would show)
   // rather than a ripple boxed into the small thumbnail.
   var onFailedAttachmentClick: ((View, Float, Float) -> Unit)? = null

   fun submitAttachments(attachments: Collection<Attachment>) {
      // Passed (and in-flight) attachments lead the carousel so a mixed pass/fail observation's
      // default (page 0) slide is always a real image, not whichever attachment happened to
      // upload first - failed ones only surface first if every attachment failed.
      this.attachments = attachments.filterNot(::isFailed) + attachments.filter(::isFailed)
      notifyDataSetChanged()
   }

   private fun isFailed(attachment: Attachment) =
      attachment.processingStatus == "rejected" || attachment.processingStatus == "error"

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
      val isUploading = attachment.isDirty && attachment.processingStatus == null
      val isPending = attachment.processingStatus == "pending"
      val isFailed = isFailed(attachment)

      holder.imageView.setImageDrawable(null)

      if (isUploading || isPending || isFailed) {
         holder.placeholderIcon.visibility = View.VISIBLE
         if (isFailed) {
            holder.placeholderIcon.setImageResource(R.drawable.ic_error_outline_24dp)
            ImageViewCompat.setImageTintList(
               holder.placeholderIcon,
               ColorStateList.valueOf(ContextCompat.getColor(context, R.color.text_primary))
            )
            holder.title.text = "Upload Failed"
            holder.title.visibility = View.VISIBLE
            holder.label.text = attachment.name
            holder.label.setTextColor(
               ColorUtils.setAlphaComponent(ContextCompat.getColor(context, R.color.text_primary), 191)
            )
            holder.label.maxLines = 1
            holder.label.ellipsize = TextUtils.TruncateAt.END
            holder.label.visibility = View.VISIBLE

            // Clickable, but with no ripple of its own - onFailedAttachmentClick plays the
            // ripple on the card underneath instead, since tapping here opens the observation,
            // the same destination a direct tap on the card leads to.
            holder.itemView.isClickable = true
            holder.itemView.isFocusable = true
            holder.itemView.foreground = null
            var touchX = 0f
            var touchY = 0f
            holder.itemView.setOnTouchListener { _, event ->
               if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                  touchX = event.x
                  touchY = event.y
               }
               false
            }
            holder.itemView.setOnClickListener { onFailedAttachmentClick?.invoke(holder.itemView, touchX, touchY) }
         } else {
            // isUploading and isPending share one treatment - the active-transfer window is so
            // brief it isn't worth a distinct label/icon, so both just read "Upload Pending".
            holder.placeholderIcon.setImageResource(R.drawable.ic_cloud_upload_24dp)
            ImageViewCompat.setImageTintList(
               holder.placeholderIcon,
               ColorStateList.valueOf(
                  ColorUtils.setAlphaComponent(ContextCompat.getColor(context, R.color.text_primary), 153)
               )
            )
            holder.title.visibility = View.GONE
            holder.label.text = "Upload Pending"
            holder.label.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            holder.label.maxLines = 2
            holder.label.ellipsize = null
            holder.label.visibility = View.VISIBLE

            holder.itemView.isClickable = false
            holder.itemView.isFocusable = false
            holder.itemView.foreground = null
            holder.itemView.setOnTouchListener(null)
            holder.itemView.setOnClickListener(null)
         }
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

      // This tap opens a different screen (the attachment viewer) rather than the observation
      // the card underneath leads to, so it keeps its own bounded ripple instead of falling
      // through to the card's.
      holder.itemView.isClickable = true
      holder.itemView.isFocusable = true
      holder.itemView.foreground = ContextCompat.getDrawable(context, selectableItemBackgroundResId)
      holder.itemView.setOnTouchListener(null)
      holder.itemView.setOnClickListener { onAttachmentClick(attachment) }
   }
}
