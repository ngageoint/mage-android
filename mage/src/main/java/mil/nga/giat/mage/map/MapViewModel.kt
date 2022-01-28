package mil.nga.giat.mage.map

import android.app.Application
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import mil.nga.giat.mage.data.feed.Feed
import mil.nga.giat.mage.data.feed.FeedItemDao
import mil.nga.giat.mage.data.feed.FeedWithItems
import mil.nga.giat.mage.data.feed.ItemWithFeed
import mil.nga.giat.mage.data.layer.LayerRepository
import mil.nga.giat.mage.data.location.LocationRepository
import mil.nga.giat.mage.data.observation.ObservationRepository
import mil.nga.giat.mage.form.Form
import mil.nga.giat.mage.glide.model.Avatar
import mil.nga.giat.mage.map.annotation.MapAnnotation
import mil.nga.giat.mage.map.preference.MapLayerPreferences
import mil.nga.giat.mage.network.Server
import mil.nga.giat.mage.network.gson.asStringOrNull
import mil.nga.giat.mage.observation.ObservationImportantState
import mil.nga.giat.mage.sdk.datastore.location.Location
import mil.nga.giat.mage.sdk.datastore.location.LocationHelper
import mil.nga.giat.mage.sdk.datastore.observation.Observation
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper
import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeature
import mil.nga.giat.mage.sdk.datastore.user.EventHelper
import mil.nga.giat.mage.sdk.datastore.user.User
import mil.nga.giat.mage.sdk.datastore.user.UserHelper
import mil.nga.giat.mage.sdk.exceptions.ObservationException
import mil.nga.giat.mage.sdk.exceptions.UserException
import mil.nga.giat.mage.sdk.utils.ISO8601DateFormatFactory
import mil.nga.giat.mage.utils.DateFormatFactory
import java.text.DateFormat
import java.util.*
import javax.inject.Inject
import kotlin.collections.set

data class FeedItemId(val feedId: String, val itemId: String)
data class StaticFeatureId(val layerId: Long, val featureId: Long)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val application: Application,
    private val mapLayerPreferences: MapLayerPreferences,
    private val feedItemDao: FeedItemDao,
    private val geocoder: Geocoder,
    private val layerRepository: LayerRepository,
    locationRepository: LocationRepository,
    observationRepository: ObservationRepository,
): ViewModel() {
    var dateFormat: DateFormat =
        DateFormatFactory.format("yyyy-MM-dd HH:mm zz", Locale.getDefault(), application)

    private val eventHelper: EventHelper = EventHelper.getInstance(application)
    private val eventId = MutableLiveData<Long>()

    val observations = observationRepository.getObservations().transform { observations ->
        val states = observations.map { observation ->
            MapAnnotation.fromObservation(observation, application)
        }

        emit(states)

    }.flowOn(Dispatchers.IO).asLiveData()

    val locations = locationRepository.getLocations().transform { locations ->
        val states = locations.map { location ->
            MapAnnotation.fromUser(location.user, location)
        }

        emit(states)
    }.flowOn(Dispatchers.IO).asLiveData()

    val featureLayers = eventId.switchMap { eventId ->
        liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            val layers = layerRepository.getStaticFeatureLayers(eventId)
            val annotations = layers.associateBy({ it.id }, { layer ->
                val features = layerRepository.getStaticFeatures(layer.id)
                val annotations = features.map { feature ->
                    MapAnnotation.fromStaticFeature(feature, application)
                }
                annotations
            })

            emit(annotations)
        }
    }

    private val _feeds = MutableLiveData<MutableMap<String, LiveData<FeedState>>>()
    val feeds: LiveData<MutableMap<String, LiveData<FeedState>>> = _feeds

    private val feedIds = MutableLiveData<Set<String>>()
    fun setEvent(id: Long) {
        eventId.value = id
        feedIds.value = mapLayerPreferences.getEnabledFeeds(id)
    }

    val items: LiveData<MutableMap<String, LiveData<FeedState>>> =
        Transformations.switchMap(feedIds) { feedIds ->
            val items = mutableMapOf<String, LiveData<FeedState>>()
            feedIds.forEach { feedId ->
                var liveData = _feeds.value?.get(feedId)
                if (liveData == null) {
                    liveData = feedItemDao.feedWithItems(feedId).map {
                        toFeedItemState(it)
                    }
                }

                items[feedId] = liveData
            }

            _feeds.value = items
            feeds
        }

    data class FeedState(val feed: Feed, val items: List<MapAnnotation<String>>)
    private fun toFeedItemState(feedWithItems: FeedWithItems): FeedState {
        val mapFeatures = feedWithItems.items.mapNotNull {
            MapAnnotation.fromFeedItem(ItemWithFeed(feedWithItems.feed, it), application)
        }
        return FeedState(feedWithItems.feed, mapFeatures)
    }

    private val searchText = MutableLiveData<String>()
    val searchResult = Transformations.switchMap(searchText) {
        liveData {
            emit(geocoder.search(it))
        }
    }

    fun search(text: String) {
        searchText.value = text
    }

    private val observationId = MutableLiveData<Long?>()
    val observationMap: LiveData<ObservationMapState?> = observationId.switchMap { id ->
        liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            if (id != null) {
                val observation = ObservationHelper.getInstance(application).read(id)
                emit(toObservationItemState(observation))
            } else {
                emit(null)
            }
        }
    }

    fun selectObservation(id: Long?) {
        observationId.value = id
    }

    private fun toObservationItemState(observation: Observation): ObservationMapState {
        val currentUser = UserHelper.getInstance(application).readCurrentUser()
        val isFavorite = if (currentUser != null) {
            val favorite = observation.favoritesMap[currentUser.remoteId]
            favorite != null && favorite.isFavorite
        } else false

        val importantState = if (observation.important?.isImportant == true) {
            val importantUser: User? = try {
                UserHelper.getInstance(application).read(observation.important?.userId)
            } catch (ue: UserException) {
                null
            }

            ObservationImportantState(
                description = observation.important?.description,
                user = importantUser?.displayName
            )
        } else null

        var primary: String? = null
        var secondary: String? = null
        if (observation.forms.isNotEmpty()) {
            val observationForm = observation.forms.first()
            val formJson = eventHelper.getForm(observationForm.formId).json
            val formDefinition = Form.fromJson(formJson)

            primary = observationForm?.properties?.find { it.key == formDefinition?.primaryMapField }?.value as? String
            secondary = observationForm?.properties?.find { it.key == formDefinition?.secondaryMapField }?.value as? String
        }

        val iconFeature = MapAnnotation.fromObservation(observation, application)

        val user = UserHelper.getInstance(application).read(observation.userId)
        val title = "${user.displayName} \u2022 ${dateFormat.format(observation.timestamp)}"

        return ObservationMapState(
            observation.id,
            title,
            observation.geometry,
            primary,
            secondary,
            iconFeature,
            isFavorite,
            importantState
        )
    }

    fun toggleFavorite(observationMapState: ObservationMapState) {
        viewModelScope.launch(Dispatchers.IO) {
            val observationHelper = ObservationHelper.getInstance(application)
            val observation = observationHelper.read(observationMapState.id)
            try {
                val user: User? = try {
                    UserHelper.getInstance(application).readCurrentUser()
                } catch (e: Exception) {
                    null
                }

                if (observationMapState.favorite) {
                    observationHelper.unfavoriteObservation(observation, user)
                } else {
                    observationHelper.favoriteObservation(observation, user)
                }

                observationId.value = observation.id
            } catch (ignore: ObservationException) {}
        }
    }

    private val locationId = MutableLiveData<Long?>()
    val location = locationId.switchMap { id ->
        liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            if (id != null) {
                val location = LocationHelper.getInstance(application).read(id)
                emit(toUserState(location.user, location))
            } else {
                emit(null)
            }
        }
    }

    fun selectUser(id: Long?) {
        locationId.value = id
    }

    private fun toUserState(user: User, location: Location): UserMapState {
        return UserMapState(
            id = user.id,
            title = dateFormat.format(location.timestamp),
            primary = user.displayName,
            geometry = location.geometry,
            image = Avatar.forUser(user),
            email = user.email,
            phone = user.primaryPhone
        )
    }

    private val feedItemId = MutableLiveData<FeedItemId?>()
    val feedItem: LiveData<FeatureMapState<FeedItemId>?> = feedItemId.switchMap { id ->
        liveData {
            if (id != null) {
                emitSource(feedItemDao.item(id.feedId, id.itemId).map {
                    feedItemToState(it)
                })
            } else {
                emit(null)
            }
        }
    }

    fun selectFeedItem(id: FeedItemId?) {
        feedItemId.value = id
    }

    private fun feedItemToState(itemWithFeed: ItemWithFeed): FeatureMapState<FeedItemId>? {
        val feed = itemWithFeed.feed
        val item = itemWithFeed.item
        val geometry = item.geometry ?: return null

        val title = item.timestamp?.let {
            dateFormat.format(it)
        }

        val primary = item.properties?.asJsonObject?.get(feed.itemPrimaryProperty)?.asStringOrNull()
        val secondary = item.properties?.asJsonObject?.get(feed.itemSecondaryProperty)?.asStringOrNull()

        return FeatureMapState(
            id = FeedItemId(feed.id, item.id),
            title = title,
            primary = primary,
            secondary = secondary,
            geometry = geometry,
            image = "${Server(application).baseUrl}/api/icons/${feed.mapStyle?.iconStyle?.id}/content"
        )
    }

    private val _userPhone = MutableLiveData<User?>()
    val userPhone: LiveData<User?> = _userPhone

    fun selectUserPhone(userState: UserMapState?) {
        val user: User? = userState?.let {
            LocationHelper.getInstance(application).read(it.id)?.user
        }

        _userPhone.value = user
    }

    private val _staticFeatureId = MutableLiveData<StaticFeatureId?>()
    val staticFeature: LiveData<StaticFeatureMapState?> = _staticFeatureId.switchMap { id ->
        liveData {
            if (id != null) {
                layerRepository.getStaticFeature(id.layerId, id.featureId)?.let { feature ->
                    emit(staticFeatureToState(feature))
                }
            } else {
                emit(null)
            }
        }
    }

    fun selectStaticFeature(feature: StaticFeatureId) {
        _staticFeatureId.value = feature
    }

    private fun staticFeatureToState(feature: StaticFeature): StaticFeatureMapState {
        val properties = feature.propertiesMap
        val timestamp = properties["timestamp"]?.value?.let { timestamp ->
            try {
                ISO8601DateFormatFactory.ISO8601().parse(timestamp)?.let { date ->
                    dateFormat.format(date)
                }
            } catch (e: Exception) { null }
        }

        return StaticFeatureMapState(
            id = feature.id,
            title = timestamp,
            primary = properties["name"]?.value,
            secondary = feature.layer.name,
            geometry = feature.geometry,
            content = properties["description"]?.value
        )
    }

    private val _geoPackageFeature = MutableLiveData<GeoPackageFeatureMapState?>()
    val geoPackageFeature:LiveData<GeoPackageFeatureMapState?> = _geoPackageFeature
    fun selectGeoPackageFeature(mapState: GeoPackageFeatureMapState?) {
        _geoPackageFeature.value = mapState
    }

    fun deselectFeature() {
        observationId.value = null
        locationId.value = null
        feedItemId.value = null
        _geoPackageFeature.value = null
        _staticFeatureId.value = null
    }
}