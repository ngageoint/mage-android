package mil.nga.giat.mage.newsfeed

import android.content.Context
import android.database.Cursor
import android.graphics.PorterDuff
import android.os.AsyncTask
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.maps.model.LatLng
import com.j256.ormlite.android.AndroidDatabaseResults
import com.j256.ormlite.stmt.PreparedQuery
import mil.nga.giat.mage.R
import mil.nga.giat.mage.coordinate.CoordinateFormatter
import mil.nga.giat.mage.map.annotation.MapAnnotation
import mil.nga.giat.mage.observation.attachment.AttachmentGallery
import mil.nga.giat.mage.sdk.datastore.observation.*
import mil.nga.giat.mage.sdk.datastore.user.EventHelper
import mil.nga.giat.mage.sdk.datastore.user.User
import mil.nga.giat.mage.sdk.datastore.user.UserHelper
import mil.nga.giat.mage.sdk.exceptions.ObservationException
import mil.nga.giat.mage.sdk.exceptions.UserException
import mil.nga.giat.mage.utils.DateFormatFactory
import java.lang.ref.WeakReference
import java.sql.SQLException
import java.util.*

class ObservationListAdapter(
   private val context: Context,
   observationFeedState: ObservationFeedViewModel.ObservationFeedState,
   private val attachmentGallery: AttachmentGallery,
   private val observationActionListener: ObservationActionListener?
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

   private inner class ObservationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
      private val card: View = view.findViewById(R.id.card)
      val markerView: ImageView = view.findViewById(R.id.observation_marker)
      val primaryView: TextView = view.findViewById(R.id.primary)
      val timeView: TextView = view.findViewById(R.id.time)
      val secondaryView: TextView = view.findViewById(R.id.secondary)
      val userView: TextView = view.findViewById(R.id.user)
      val importantView: View = view.findViewById(R.id.important)
      val importantOverline: TextView = view.findViewById(R.id.important_overline)
      val importantDescription: TextView = view.findViewById(R.id.important_description)
      val syncBadge: View = view.findViewById(R.id.sync_status)
      val errorBadge: View = view.findViewById(R.id.error_status)
      val attachmentLayout: LinearLayout = view.findViewById(R.id.image_gallery)
      val locationView: TextView = view.findViewById(R.id.location)
      val locationContainer: View = view.findViewById(R.id.location_container)
      val favoriteButton: ImageView = view.findViewById(R.id.favorite_button)
      val favoriteCount: TextView = view.findViewById(R.id.favorite_count)
      val directionsButton: View = view.findViewById(R.id.directions_button)
      var userTask: UserTask? = null
      var primaryPropertyTask: PropertyTask? = null
      var secondaryPropertyTask: PropertyTask? = null

      fun bind(observation: Observation) {
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

   override fun getItemCount(): Int {
      return cursor.count + 1
   }

   override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
      if (holder is ObservationViewHolder) {
         if (holder.userTask != null) {
            holder.userTask?.cancel(false)
         }
         if (holder.primaryPropertyTask != null) {
            holder.primaryPropertyTask?.cancel(false)
         }
         if (holder.secondaryPropertyTask != null) {
            holder.secondaryPropertyTask?.cancel(false)
         }
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

         Glide.with(context)
            .asBitmap()
            .load(MapAnnotation.fromObservation(observation, context))
            .error(R.drawable.default_marker)
            .into(vh.markerView)

         vh.primaryView.text = ""
         vh.primaryPropertyTask = PropertyTask(context, PropertyTask.Type.PRIMARY, vh.primaryView)
         vh.primaryPropertyTask?.execute(observation)

         vh.secondaryView.text = ""
         vh.secondaryPropertyTask = PropertyTask(context, PropertyTask.Type.SECONDARY, vh.secondaryView)
         vh.secondaryPropertyTask?.execute(observation)

         vh.userView.text = ""
         vh.userTask = UserTask(vh.userView)
         vh.userTask?.execute(observation)

         val timestamp = observation.timestamp
         val dateFormat = DateFormatFactory.format("yyyy-MM-dd HH:mm zz", Locale.getDefault(), context)
         vh.timeView.text = dateFormat.format(timestamp)

         setImportantView(observation.important, vh)
         val error = observation.error
         if (error != null) {
            vh.errorBadge.visibility = if (error.statusCode != null) View.VISIBLE else View.GONE
         } else {
            vh.syncBadge.visibility = if (observation.isDirty) View.VISIBLE else View.GONE
            vh.errorBadge.visibility = View.GONE
         }

         vh.attachmentLayout.removeAllViews()
         if (observation.attachments.isEmpty()) {
            vh.attachmentLayout.visibility = View.GONE
         } else {
            vh.attachmentLayout.visibility = View.VISIBLE
            attachmentGallery.addAttachments(vh.attachmentLayout, observation.attachments)
         }

         val centroid = observation.geometry.centroid
         val coordinates = CoordinateFormatter(context).format(LatLng(centroid.y, centroid.x))
         vh.locationView.text = coordinates
         vh.locationContainer.setOnClickListener { onLocationClick(observation) }

         vh.favoriteButton.setOnClickListener { toggleFavorite(observation, vh) }
         setFavoriteImage(observation.favorites, vh, isFavorite(observation))

         vh.directionsButton.setOnClickListener { getDirections(observation) }
      } catch (e: SQLException) {
         e.printStackTrace()
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

   private fun setImportantView(important: ObservationImportant?, vh: ObservationViewHolder) {
      val isImportant = important != null && important.isImportant
      vh.importantView.visibility = if (isImportant) View.VISIBLE else View.GONE
      if (isImportant) {
         try {
            val user = UserHelper.getInstance(context).read(important!!.userId)
            vh.importantOverline.text = String.format("FLAGGED BY %s", user.displayName.uppercase(Locale.getDefault()))
         } catch (e: UserException) {
            e.printStackTrace()
         }
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
      val observationHelper = ObservationHelper.getInstance(context)
      val isFavorite = isFavorite(observation)
      try {
         if (isFavorite) {
            observationHelper.unfavoriteObservation(observation, currentUser)
         } else {
            observationHelper.favoriteObservation(observation, currentUser)
         }
         setFavoriteImage(observation.favorites, vh, isFavorite)
      } catch (e: ObservationException) {
         Log.e(LOG_NAME, "Could not unfavorite observation", e)
      }
   }

   private fun isFavorite(observation: Observation): Boolean {
      var isFavorite = false
      try {
         currentUser = UserHelper.getInstance(context).readCurrentUser()
         if (currentUser != null) {
            val favorite = observation.favoritesMap[currentUser!!.remoteId]
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

   internal inner class UserTask(textView: TextView) : AsyncTask<Observation?, Void?, User?>() {
      private val reference: WeakReference<TextView> = WeakReference(textView)

      override fun doInBackground(vararg observations: Observation?): User? {
         var user: User? = null
         try {
            user = UserHelper.getInstance(context).read(observations[0]?.userId)
         } catch (e: UserException) {
            Log.e(LOG_NAME, "Could not get user", e)
         }
         return user
      }

      override fun onPostExecute(u: User?) {
         val user = if (isCancelled) null else u

         val textView = reference.get()
         if (textView != null) {
            if (user != null) {
               textView.text = user.displayName
            } else {
               textView.text = "Unknown User"
            }
         }
      }
   }

   private class PropertyTask(private val context: Context, private val type: Type, textView: TextView) : AsyncTask<Observation?, Void?, ObservationProperty>() {
      enum class Type { PRIMARY, SECONDARY }

      private val reference: WeakReference<TextView> = WeakReference(textView)

      override fun doInBackground(vararg observations: Observation?): ObservationProperty? {
         val field = observations[0]?.forms?.firstOrNull()?.let { observationForm ->
            val form = EventHelper.getInstance(context).getForm(observationForm.formId)
            val fieldName = if (type == Type.PRIMARY) form.primaryFeedField else form.secondaryFeedField
            observationForm.properties.find { it.key == fieldName }
         }

         return field
      }

      override fun onPostExecute(p: ObservationProperty?) {
         val property = if (isCancelled) null else p

         val textView = reference.get()
         if (textView != null) {
            if (property == null || property.isEmpty) {
               textView.visibility = View.GONE
            } else {
               textView.text = property.value.toString()
               textView.visibility = View.VISIBLE
            }
            textView.requestLayout()
         }
      }
   }

   companion object {
      private val LOG_NAME = ObservationListAdapter::class.java.name
      private const val TYPE_OBSERVATION = 1
      private const val TYPE_FOOTER = 2
   }
}