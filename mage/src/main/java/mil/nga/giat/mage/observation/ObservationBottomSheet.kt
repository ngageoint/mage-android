package mil.nga.giat.mage.observation

import android.content.Context
import android.text.format.DateUtils
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import mil.nga.giat.mage.R
import mil.nga.giat.mage.databinding.ViewObservationBottomSheetBinding
import mil.nga.giat.mage.map.marker.ObservationBitmapFactory
import mil.nga.giat.mage.newsfeed.ObservationListAdapter
import mil.nga.giat.mage.sdk.datastore.observation.Observation
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper
import mil.nga.giat.mage.sdk.datastore.user.UserHelper
import mil.nga.giat.mage.sdk.exceptions.ObservationException
import mil.nga.giat.mage.sdk.exceptions.UserException
import java.text.SimpleDateFormat
import java.util.*

const val SHORT_TIME_PATTERN = "h:mm a"
const val SHORT_DATE_PATTERN = "MMM d"
val LOG_NAME = ObservationBottomSheet::class.simpleName ?: "ObservationBottomSheet"

@BindingAdapter("app:observation")
fun ObservationBottomSheet.bindObservation(observation: Observation?) {
    binding.observation = observation
    binding.observationUser = null
    binding.importantUser = null
    observation?.let {
        binding.observationUser = UserHelper.getInstance(context).read(it.userId)
        if (it.important?.isImportant == true) {
            binding.importantUser = UserHelper.getInstance(context).read(it.important?.userId)
        }
        val pattern =
            if (DateUtils.isToday(it.timestamp.time)) SHORT_TIME_PATTERN else SHORT_DATE_PATTERN
        binding.observationDateFormatted =
            SimpleDateFormat(pattern, Locale.getDefault()).format(it.timestamp)
        binding.observationMarker.setImageBitmap(
            ObservationBitmapFactory.bitmap(context, it)
        )

        binding.primary.visibility = if (it.primaryFeedField?.value != null) View.VISIBLE else View.GONE
        binding.secondary.visibility = if (it.secondaryFeedField?.value != null) View.VISIBLE else View.GONE
        binding.important.visibility = if (it.important?.isImportant == true) View.VISIBLE else View.GONE
        val error = it.error
        if (error != null) {
            val hasValidationError = error.statusCode != null;
            binding.syncStatus.visibility = if (hasValidationError) View.GONE else View.VISIBLE
            binding.errorStatus.visibility = if (hasValidationError) View.VISIBLE else View.GONE
        } else {
            binding.syncStatus.visibility = View.GONE
            binding.errorStatus.visibility = View.GONE
        }
        setFavoriteImage(it)
    }
}
class ObservationBottomSheet @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
        defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyle, defStyleRes) {
    val binding: ViewObservationBottomSheetBinding = ViewObservationBottomSheetBinding.inflate(LayoutInflater.from(context), this, true)
    var observationActionListener: ObservationListAdapter.ObservationActionListener? = null

    init {
        binding.directionsButton.setOnClickListener {
            observationActionListener?.onObservationDirections(binding.observation!!)
        }
        binding.moreDetailsButton.setOnClickListener {
            observationActionListener?.onObservationClick(binding.observation!!)
        }
        binding.favoriteButton.setOnClickListener {
            binding.observation?.let {
                toggleFavorite(it)
            }
        }
    }

    fun setFavoriteImage(observation: Observation) {
        var isFavorite = false
        try {
            val currentUser = UserHelper.getInstance(context).readCurrentUser()
            if (currentUser != null) {
                val favorite = observation.favoritesMap[currentUser.getRemoteId()]
                isFavorite = favorite?.isFavorite == true
            }
        } catch (e: UserException) {
            Log.e(LOG_NAME, "Could not get user", e)
        }
        if (isFavorite) {
            binding.favoriteButton.setImageDrawable(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_favorite_white_24dp
                )
            )
            binding.favoriteButton.setColorFilter(
                ContextCompat.getColor(
                    context,
                    R.color.observation_favorite_active
                )
            )
        } else {
            binding.favoriteButton.setImageDrawable(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_favorite_border_white_24dp
                )
            )
            binding.favoriteButton.setColorFilter(
                ContextCompat.getColor(
                    context,
                    R.color.observation_favorite_inactive
                )
            )
        }
        val favorites = observation.favorites
        val favoriteCount = favorites.count { it.isFavorite }
        binding.favoriteCount.visibility = if (favoriteCount > 0) View.VISIBLE else View.GONE
        binding.favoriteCount.text = String.format(Locale.getDefault(), "%d", favoriteCount)
    }

    private fun toggleFavorite(observation: Observation) {
        val observationHelper = ObservationHelper.getInstance(context)
        val isFavorite = isFavorite(observation)
        val currentUser = UserHelper.getInstance(context).readCurrentUser()
        try {
            if (isFavorite) {
                observationHelper.unfavoriteObservation(observation, currentUser)
            } else {
                observationHelper.favoriteObservation(observation, currentUser)
            }
            setFavoriteImage(observation)
        } catch (e: ObservationException) {
            Log.e(LOG_NAME, "Could not unfavorite observation", e)
        }
    }

    private fun isFavorite(observation: Observation): Boolean {
        var isFavorite = false
        try {
            val currentUser = UserHelper.getInstance(context).readCurrentUser()
            if (currentUser != null) {
                val favorite = observation.favoritesMap[currentUser.getRemoteId()]
                isFavorite = favorite?.isFavorite == true
            }
        } catch (e: UserException) {
            Log.e(LOG_NAME, "Could not get user", e)
        }
        return isFavorite
    }
}