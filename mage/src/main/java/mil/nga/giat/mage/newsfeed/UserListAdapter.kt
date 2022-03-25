package mil.nga.giat.mage.newsfeed

import android.content.Context
import android.database.Cursor
import android.graphics.PorterDuff
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.LatLng
import com.google.common.collect.Collections2
import com.j256.ormlite.android.AndroidDatabaseResults
import com.j256.ormlite.stmt.PreparedQuery
import mil.nga.giat.mage.R
import mil.nga.giat.mage.coordinate.CoordinateFormatter
import mil.nga.giat.mage.glide.GlideApp
import mil.nga.giat.mage.glide.model.Avatar.Companion.forUser
import mil.nga.giat.mage.sdk.datastore.location.Location
import mil.nga.giat.mage.sdk.datastore.location.LocationHelper
import mil.nga.giat.mage.sdk.datastore.user.EventHelper
import mil.nga.giat.mage.sdk.datastore.user.Team
import mil.nga.giat.mage.sdk.datastore.user.TeamHelper
import mil.nga.giat.mage.sdk.datastore.user.User
import mil.nga.giat.mage.utils.DateFormatFactory
import mil.nga.sf.util.GeometryUtils
import org.apache.commons.lang3.StringUtils
import java.sql.SQLException
import java.text.DateFormat
import java.util.*

sealed class UserAction {
   class Coordinates(val location: String): UserAction()
   class Email(val email: String): UserAction()
   class Phone(val phone: String): UserAction()
   class Directions(val user: User, val location: Location): UserAction()
}

class UserListAdapter(
   private val context: Context,
   userFeedState: UserFeedState,
   private val userAction: (UserAction) -> Unit,
   private val userClickListener: (User) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
   val dateFormat: DateFormat = DateFormatFactory.format("yyyy-MM-dd HH:mm zz", Locale.getDefault(), context)

   private var cursor: Cursor = userFeedState.cursor
   private val query: PreparedQuery<Location> = userFeedState.query
   private val filterText: String = userFeedState.filterText
   private val teamHelper = TeamHelper.getInstance(context)
   private val eventHelper = EventHelper.getInstance(context)

   private class PersonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
      private val cardView: View = view.findViewById(R.id.card)
      val avatarView: ImageView = view.findViewById(R.id.avatarImageView)
      val nameView: TextView = view.findViewById(R.id.name)
      val dateView: TextView = view.findViewById(R.id.date)
      val teamsView: TextView = view.findViewById(R.id.teams)
      val location: TextView = view.findViewById(R.id.location)
      val email: ImageButton = view.findViewById(R.id.email_button)
      val phone: ImageButton = view.findViewById(R.id.phone_button)
      val directions: ImageButton = view.findViewById(R.id.directions_button)

      fun bind(user: User, listener: (User) -> Unit) {
         cardView.setOnClickListener { listener.invoke(user) }
      }
   }

   private class FooterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
      val footerText: TextView = view.findViewById(R.id.footer_text)
   }

   override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
      return if (viewType == TYPE_USER) {
         val itemView = LayoutInflater.from(parent.context).inflate(R.layout.people_list_item, parent, false)
         PersonViewHolder(itemView)
      } else {
         val itemView = LayoutInflater.from(parent.context).inflate(R.layout.feed_footer, parent, false)
         FooterViewHolder(itemView)
      }
   }

   override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
      when (holder) {
         is PersonViewHolder -> bindUser(holder, position)
         else -> bindFooter(holder)
      }
   }

   private fun bindUser(holder: RecyclerView.ViewHolder, position: Int) {
      cursor.moveToPosition(position)

      val vh = holder as PersonViewHolder
      try {
         val location = query.mapRow(AndroidDatabaseResults(cursor, null, false))
         val user = location.user ?: return
         vh.bind(user, userClickListener)

         val defaultPersonIcon = DrawableCompat.wrap(ContextCompat.getDrawable(context, R.drawable.ic_person_white_48dp)!!)
         DrawableCompat.setTint(defaultPersonIcon, ContextCompat.getColor(context, R.color.icon))
         DrawableCompat.setTintMode(defaultPersonIcon, PorterDuff.Mode.SRC_ATOP)
         vh.avatarView.setImageDrawable(defaultPersonIcon)

         GlideApp.with(context)
            .load(forUser(user))
            .fallback(defaultPersonIcon)
            .error(defaultPersonIcon)
            .circleCrop()
            .into(vh.avatarView)

         vh.nameView.text = user.displayName
         val timeText = dateFormat.format(location.timestamp)

         vh.dateView.text = timeText

         val userTeams: MutableCollection<Team> = teamHelper.getTeamsByUser(user)
         val event = eventHelper.currentEvent
         val eventTeams = teamHelper.getTeamsByEvent(event)
         userTeams.retainAll(eventTeams.toSet())
         val teamNames = Collections2.transform(userTeams) { team: Team -> team.name }
         vh.teamsView.text = StringUtils.join(teamNames, ", ")

         val locations = LocationHelper.getInstance(context).getUserLocations(user.id, event.id, 1, true)
         locations?.first()?.let { location ->
            val point = GeometryUtils.getCentroid(location.geometry)
            val coordinates = CoordinateFormatter(context).format(LatLng(point.y, point.x))
            vh.location.text = coordinates
            vh.location.setOnClickListener { userAction(UserAction.Coordinates(coordinates)) }
            vh.directions.setOnClickListener { userAction(UserAction.Directions(user, location)) }
         }

         if (user.email?.isNotEmpty() == true) {
            vh.email.visibility = View.VISIBLE
            vh.email.setOnClickListener { userAction(UserAction.Email(user.email)) }
         } else {
            vh.email.visibility = View.GONE
         }

         if (user.primaryPhone?.isNotEmpty() == true) {
            vh.phone.visibility = View.VISIBLE
            vh.phone.setOnClickListener { userAction(UserAction.Phone(user.primaryPhone)) }
         } else {
            vh.phone.visibility = View.GONE
         }
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Could not set location view information.", e)
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

   override fun getItemViewType(position: Int): Int {
      return if (position == cursor.count) {
         TYPE_FOOTER
      } else {
         TYPE_USER
      }
   }

   override fun getItemCount(): Int {
      return cursor.count + 1
   }

   companion object {
      private val LOG_NAME = UserListAdapter::class.java.name
      private const val TYPE_USER = 1
      private const val TYPE_FOOTER = 2
   }
}