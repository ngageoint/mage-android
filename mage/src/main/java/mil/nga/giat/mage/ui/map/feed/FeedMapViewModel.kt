package mil.nga.giat.mage.ui.map.feed

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MarkerState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.transform
import mil.nga.giat.mage.R
import mil.nga.giat.mage.data.repository.feed.FeedRepository
import mil.nga.giat.mage.data.repository.map.resizeBitmapToWidthAspectScaled
import mil.nga.giat.mage.database.model.feed.ItemWithFeed
import mil.nga.giat.mage.map.annotation.MapAnnotation
import mil.nga.giat.mage.ui.map.IconMarkerState
import mil.nga.sf.LineString
import mil.nga.sf.Point
import mil.nga.sf.Polygon
import kotlin.coroutines.suspendCoroutine

@HiltViewModel(assistedFactory = FeedMapViewModel.FeedMapViewModelFactory::class)
class FeedMapViewModel @AssistedInject constructor(
    @Assisted val id: String,
    feedRepository: FeedRepository,
    private val application: Application
) : ViewModel() {
    @AssistedFactory
    interface FeedMapViewModelFactory {
        fun create(id: String): FeedMapViewModel
    }

    private var feedStates = mutableMapOf<String, IconMarkerState>()

    val width = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        32.0f,
        application.resources.displayMetrics
    )

    val feedItems = feedRepository.getFeedItems(id).asFlow().transform { feedWithItems ->
        val newStates = mutableMapOf<String, IconMarkerState>()
        feedWithItems.items.map { feedItem ->
            var state = feedStates[feedItem.id]
            if (state == null) {
                MapAnnotation.fromFeedItem(ItemWithFeed(feedWithItems.feed, feedItem), application)?.let { annotation ->
                    val geometry = annotation.geometry
                    when (geometry) {
                        is Point -> {

                            val icon = loadIcon(application, annotation, R.drawable.default_marker)
                            newStates[feedItem.id] = IconMarkerState(
                                markerState = MarkerState(
                                    position = LatLng(geometry.y, geometry.x)
                                ),
                                icon = icon?.let {
                                    BitmapDescriptorFactory.fromBitmap(it.resizeBitmapToWidthAspectScaled(width)) },
                                id = feedItem.id
                            )
                        }

                        is LineString -> {
                            null
                        }

                        is Polygon -> {
                            null
                        }

                        else -> {
                            null
                        }
                    }
                }
                state = newStates[feedItem.id]
            }
            state?.let {
                newStates[feedItem.id] = state
            }

        }
        emit(newStates.values.toList())
    }.flowOn(Dispatchers.IO)

    private suspend fun loadIcon(
        context: Context,
        annotation: MapAnnotation<*>?,
        placeHolder: Int,
    ): Bitmap? {
        // TODO: Not 100% sure this is the best way to do this
        return suspendCoroutine { continuation ->
            try {
                Glide.with(context)
                    .asBitmap()
                    .load(annotation)
                    .error(placeHolder)
                    // to show a default icon in case of any errors
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap>?
                        ) {
                            continuation.resumeWith(Result.success(resource))
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {

                        }
                    })
            } catch (e: Exception) {
                e.printStackTrace()
                continuation.resumeWith(Result.failure(e))
            }
        }
    }
}