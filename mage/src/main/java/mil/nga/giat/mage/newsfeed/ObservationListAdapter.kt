package mil.nga.giat.mage.newsfeed

import android.content.Context
import android.database.Cursor
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.chip.Chip
import com.j256.ormlite.android.AndroidDatabaseResults
import com.j256.ormlite.stmt.PreparedQuery
import mil.nga.giat.mage.R
import mil.nga.giat.mage.coordinate.CoordinateFormatter
import mil.nga.giat.mage.database.model.observation.Attachment
import mil.nga.giat.mage.database.model.observation.Observation
import mil.nga.giat.mage.database.model.observation.ObservationFavorite
import mil.nga.giat.mage.data.datasource.observation.ObservationLocalDataSource
import mil.nga.giat.mage.database.model.observation.ObservationImportant
import mil.nga.giat.mage.database.model.observation.ObservationProperty
import mil.nga.giat.mage.map.annotation.MapAnnotation
import mil.nga.giat.mage.observation.attachment.AttachmentCarouselAdapter
import mil.nga.giat.mage.data.datasource.event.EventLocalDataSource
import mil.nga.giat.mage.database.model.user.User
import mil.nga.giat.mage.data.datasource.user.UserLocalDataSource
import mil.nga.giat.mage.sdk.exceptions.ObservationException
import mil.nga.giat.mage.sdk.exceptions.UserException
import mil.nga.giat.mage.utils.DateFormatFactory
import mil.nga.sf.Point
import java.sql.SQLException
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ObservationListAdapter(
   private val context: Context,
   private val userLocalDataSource: UserLocalDataSource,
   private val eventLocalDataSource: EventLocalDataSource,
   private val observationLocalDataSource: ObservationLocalDataSource,
   observationFeedState: ObservationFeedViewModel.ObservationFeedState,
   private val onAttachmentClick: (Attachment) -> Unit,
   private val observationActionListener: ObservationActionListener?,
   private val scope: CoroutineScope
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

   interface ObservationActionListener {
      fun onObservationClick(observation: Observation)
      fun onObservationDirections(observation: Observation)
      fun onObservationLocation(observation: Observation)
   }

   private var cursor: Cursor = observationFeedState.cursor
   private val query: PreparedQuery<Observation> = observationFeedState.query
   private val filterText: String = observationFeedState.filterText
   private var currentUser: User? = null
   private val event = eventLocalDataSource.currentEvent

   private inner class ObservationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
      private val card: View = view.findViewById(R.id.card)
      val markerView: ImageView = view.findViewById(R.id.observation_marker)
      val primaryView: TextView = view.findViewById(R.id.primary)
      val secondaryView: TextView = view.findViewById(R.id.secondary)
      val userView: TextView = view.findViewById(R.id.user)
      val importantView: View = view.findViewById(R.id.important)
      val importantOverline: TextView = view.findViewById(R.id.important_overline)
      val importantDescription: TextView = view.findViewById(R.id.important_description)
      val syncBadge: View = view.findViewById(R.id.sync_status)
      val errorBadge: View = view.findViewById(R.id.error_status)
      val attachmentCarousel: RecyclerView = view.findViewById(R.id.attachment_carousel)
      val attachmentDots: LinearLayout = view.findViewById(R.id.attachment_dots)
      val attachmentPageCount: Chip = view.findViewById(R.id.attachment_page_count)
      val attachmentFailedBadge: Chip = view.findViewById(R.id.attachment_failed_badge)
      var dotViews: List<View> = emptyList()
      val locationView: TextView = view.findViewById(R.id.location)
      val locationContainer: View = view.findViewById(R.id.location_container)
      val favoriteButton: ImageView = view.findViewById(R.id.favorite_button)
      val favoriteCount: TextView = view.findViewById(R.id.favorite_count)
      val directionsButton: View = view.findViewById(R.id.directions_button)
      val timeView: TextView = view.findViewById(R.id.time)

      val attachmentCarouselAdapter = AttachmentCarouselAdapter(context, onAttachmentClick)
      var totalAttachmentCount = 0

      var timestamp: Date? = null
      var centroid: Point? = null
      var bindJob: Job? = null

      init {
         attachmentCarousel.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
         attachmentCarousel.adapter = attachmentCarouselAdapter
         PagerSnapHelper().attachToRecyclerView(attachmentCarousel)
         attachmentCarousel.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
               val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
               val position = layoutManager.findFirstVisibleItemPosition()
               if (position != RecyclerView.NO_POSITION && totalAttachmentCount > 1) {
                  attachmentPageCount.text = "${position + 1} of $totalAttachmentCount"
                  updateActiveDot(position)
               }
            }
         })
      }

      fun setupDots(count: Int) {
         attachmentDots.removeAllViews()
         if (count <= 1) {
            attachmentDots.visibility = View.GONE
            dotViews = emptyList()
            return
         }
         attachmentDots.visibility = View.VISIBLE
         val dotSize = (6 * context.resources.displayMetrics.density).toInt()
         val dotMargin = (3 * context.resources.displayMetrics.density).toInt()
         dotViews = (0 until count).map { index ->
            val dot = View(context)
            val params = LinearLayout.LayoutParams(dotSize, dotSize)
            params.setMargins(dotMargin, 0, dotMargin, 0)
            dot.layoutParams = params
            dot.background = dotDrawable(index == 0)
            attachmentDots.addView(dot)
            dot
         }
      }

      fun updateActiveDot(position: Int) {
         dotViews.forEachIndexed { index, dot ->
            dot.background = dotDrawable(index == position)
         }
      }

      private fun dotDrawable(active: Boolean): GradientDrawable {
         return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(if (active) Color.WHITE else Color.argb(102, 255, 255, 255))
         }
      }

      fun bind(observation: Observation) {
         timestamp = observation.timestamp
         centroid = observation.geometry.centroid

         card.setOnClickListener { observationActionListener?.onObservationClick(observation) }
      }
   }

   private inner class FooterViewHolder(view: View) :
      RecyclerView.ViewHolder(view) {
      val footerText: TextView = view.findViewById(R.id.footer_text)

   }

   override fun getItemViewType(position: Int): Int {
      return if (position == cursor.count) {
         TYPE_FOOTER
      } else {
         TYPE_OBSERVATION
      }
   }

   override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
      return if (viewType == TYPE_OBSERVATION) {
         val itemView = LayoutInflater.from(parent.context).inflate(R.layout.observation_list_item, parent, false)
         ObservationViewHolder(itemView)
      } else {
         val itemView = LayoutInflater.from(parent.context).inflate(R.layout.feed_footer, parent, false)
         FooterViewHolder(itemView)
      }
   }

   override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
      when (holder) {
         is ObservationViewHolder -> bindObservation(holder, position)
         else -> bindFooter(holder)
      }
   }

   override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: List<Any>) {
      if (payloads.isEmpty()) {
         super.onBindViewHolder(holder, position, payloads)
      } else {
         if (holder is ObservationViewHolder) {
            for (payload in payloads) {
               if (payload == PAYLOAD_TIMEZONE_CHANGE) {
                  updateTimeZoneDisplay(holder)
               }
               if (payload == PAYLOAD_COORDINATE_CHANGE) {
                  updateCoordinateDisplay(holder)
               }
            }
         }
      }
   }

   private fun updateTimeZoneDisplay(vh: ObservationViewHolder) {
      vh.timestamp?.let { timestamp ->
         val dateFormat = DateFormatFactory.format("yyyy-MM-dd HH:mm zz", Locale.getDefault(), context)
         vh.timeView.text = dateFormat.format(timestamp)
      }
   }

   private fun updateCoordinateDisplay(vh: ObservationViewHolder) {
      vh.centroid?.let { point ->
         val coordinates = CoordinateFormatter(context).format(LatLng(point.y, point.x))
         vh.locationView.text = coordinates
      }
   }

   override fun getItemCount(): Int {
      return cursor.count + 1
   }

   override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
      if (holder is ObservationViewHolder) {
          holder.bindJob?.cancel()
      }
   }


   private fun bindObservation(holder: RecyclerView.ViewHolder, position: Int) {
      cursor.moveToPosition(position)

      val vh = holder as ObservationViewHolder

      try {
         val observation = query.mapRow(AndroidDatabaseResults(cursor, null, false))
         vh.bind(observation)

         val markerPlaceholder = DrawableCompat.wrap(ContextCompat.getDrawable(context, R.drawable.ic_place_white_48dp)!!)
         DrawableCompat.setTint(markerPlaceholder, ContextCompat.getColor(context, R.color.icon))
         DrawableCompat.setTintMode(markerPlaceholder, PorterDuff.Mode.SRC_IN)
         vh.markerView.setImageDrawable(markerPlaceholder)

         val observationForm = observation.forms.firstOrNull()
         val formDefinition = observationForm?.formId?.let { formId ->
            eventLocalDataSource.getForm(formId)
         }

         val icon = MapAnnotation.getAnnotationWithStyleFromObservation(
            event = event,
            observation = observation,
            formDefinition = formDefinition,
            observationForm = observationForm,
            geometryType = observation.geometry.geometryType,
            context = context
         )

         Glide.with(context)
            .asBitmap()
            .load(icon)
            .error(R.drawable.default_marker)
            .into(vh.markerView)

         vh.primaryView.text = ""
         vh.secondaryView.text = ""
         vh.userView.text = "" 

         // Cancel any in-flight load from a previous binding of this ViewHolder
         vh.bindJob?.cancel()
         vh.bindJob = scope.launch {
               // Fetch user name from local DB on the IO thread
               val user = withContext(Dispatchers.IO) {
                  try { userLocalDataSource.read(observation.userId) } catch (e: Exception) { null }
               }   
               
               // Load the form definition once — primary and secondary fields both need it
               val observationForm = observation.forms.firstOrNull()
               val form = observationForm?.let {
                  withContext(Dispatchers.IO) { eventLocalDataSource.getForm(it.formId) }
               }   
               
               // Property look-ups are in-memory once the form is loaded
               val primaryProperty = observationForm?.properties?.find { it.key == form?.primaryFeedField }
               val secondaryProperty = observationForm?.properties?.find { it.key == form?.secondaryFeedField }
               
               val importantUserName = withContext(Dispatchers.IO) {
                  try {
                     observation.important?.userId?.let { userLocalDataSource.read(it)?.displayName }
                  } catch (e: Exception) { null }
               }

               // Everything below runs back on the main thread (scope is Main)
               vh.userView.text = user?.displayName ?: "Unknown User"

               if (primaryProperty == null || primaryProperty.isEmpty) {
                  vh.primaryView.visibility = View.GONE
               } else {
                  vh.primaryView.text = primaryProperty.value.toString()
                  vh.primaryView.visibility = View.VISIBLE
               }
               vh.primaryView.requestLayout()

               if (secondaryProperty == null || secondaryProperty.isEmpty) {
                  vh.secondaryView.visibility = View.GONE
               } else {
                  vh.secondaryView.text = secondaryProperty.value.toString()
                  vh.secondaryView.visibility = View.VISIBLE
               }
               vh.secondaryView.requestLayout()

               setImportantView(observation.important, importantUserName, vh)
         }

         updateTimeZoneDisplay(vh)

         val error = observation.error
         if (error != null) {
            vh.errorBadge.visibility = if (error.statusCode != null) View.VISIBLE else View.GONE
         } else {
            vh.syncBadge.visibility = if (observation.isDirty) View.VISIBLE else View.GONE
            vh.errorBadge.visibility = View.GONE
         }

         vh.attachmentCarouselAdapter.submitAttachments(observation.attachments)
         vh.totalAttachmentCount = observation.attachments.size
         vh.attachmentCarousel.scrollToPosition(0)
         vh.setupDots(observation.attachments.size)
         if (observation.attachments.size > 1) {
            vh.attachmentPageCount.text = "1 of ${observation.attachments.size}"
            vh.attachmentPageCount.visibility = View.VISIBLE
         } else {
            vh.attachmentPageCount.visibility = View.GONE
         }

         val failedAttachmentCount = observation.attachments.count {
            it.processingStatus == "rejected" || it.processingStatus == "error"
         }
         if (failedAttachmentCount > 0) {
            vh.attachmentFailedBadge.text = if (failedAttachmentCount > 1) "Attachments Failed - $failedAttachmentCount" else "Attachment Failed"
            vh.attachmentFailedBadge.visibility = View.VISIBLE
         } else {
            vh.attachmentFailedBadge.visibility = View.GONE
         }

         updateCoordinateDisplay(vh)
         vh.locationContainer.setOnClickListener { onLocationClick(observation) }

         vh.favoriteButton.setOnClickListener { toggleFavorite(observation, vh) }
         setFavoriteImage(observation.favorites, vh, isFavorite(observation))

         vh.directionsButton.setOnClickListener { getDirections(observation) }
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Error reading observation from database", e)
      }
   }

   private fun bindFooter(holder: RecyclerView.ViewHolder) {
      val vh = holder as FooterViewHolder
      var footerText = "End of results"
      if (filterText.isNotEmpty()) {
         footerText = "End of results for $filterText"
      }
      vh.footerText.text = footerText
   }

   private fun setImportantView(important: ObservationImportant?, flaggedByName: String?, vh: ObservationViewHolder) {
      val isImportant = important != null && important.isImportant
      vh.importantView.visibility = if (isImportant) View.VISIBLE else View.GONE
      if (isImportant) {
         vh.importantOverline.text = String.format("FLAGGED BY %s", flaggedByName?.uppercase(Locale.getDefault()) ?: "UNKNOWN")
         vh.importantDescription.text = important!!.description
      }
   }

   private fun setFavoriteImage(favorites: Collection<ObservationFavorite>, vh: ObservationViewHolder, isFavorite: Boolean) {
      if (isFavorite) {
         vh.favoriteButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_favorite_white_24dp))
         vh.favoriteButton.setColorFilter(ContextCompat.getColor(context, R.color.observation_favorite_active))
      } else {
         vh.favoriteButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_favorite_border_white_24dp))
         vh.favoriteButton.setColorFilter(ContextCompat.getColor(context, R.color.observation_favorite_inactive))
      }

      vh.favoriteCount.visibility = if (favorites.isNotEmpty()) View.VISIBLE else View.GONE
      vh.favoriteCount.text = String.format(Locale.getDefault(), "%d", favorites.size)
   }

   private fun toggleFavorite(observation: Observation, vh: ObservationViewHolder) {
      val isFavorite = isFavorite(observation)
      userLocalDataSource.readCurrentUser()?.let { user ->
         try {
            if (isFavorite) {
               observationLocalDataSource.unfavoriteObservation(observation, user)
            } else {
               observationLocalDataSource.favoriteObservation(observation, user)
            }
            setFavoriteImage(observation.favorites, vh, isFavorite)
         } catch (e: ObservationException) {
            Log.e(LOG_NAME, "Could not unfavorite observation", e)
         }
      }
   }

   private fun isFavorite(observation: Observation): Boolean {
      var isFavorite = false
      try {
         userLocalDataSource.readCurrentUser()?.let { user ->
            currentUser = user
            val favorite = observation.favoritesMap[user.remoteId]
            isFavorite = favorite != null && favorite.isFavorite
         }
      } catch (e: UserException) {
         Log.e(LOG_NAME, "Could not get user", e)
      }
      return isFavorite
   }

   private fun getDirections(observation: Observation) {
      observationActionListener?.onObservationDirections(observation)
   }

   private fun onLocationClick(observation: Observation) {
      observationActionListener?.onObservationLocation(observation)
   }

   companion object {
      private val LOG_NAME = ObservationListAdapter::class.java.name
      private const val TYPE_OBSERVATION = 1
      private const val TYPE_FOOTER = 2
      const val PAYLOAD_TIMEZONE_CHANGE = "PAYLOAD_TIMEZONE_CHANGE"
      const val PAYLOAD_COORDINATE_CHANGE = "PAYLOAD_COORDINATE_CHANGE"
   }
}