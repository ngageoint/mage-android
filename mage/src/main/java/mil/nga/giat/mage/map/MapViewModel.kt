package mil.nga.giat.mage.map

import android.app.Application
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import mil.nga.giat.mage.search.Geocoder
import mil.nga.giat.mage.search.SearchResponse
import mil.nga.giat.mage.database.model.feed.Feed
import mil.nga.giat.mage.database.dao.feed.FeedItemDao
import mil.nga.giat.mage.database.model.feed.FeedWithItems
import mil.nga.giat.mage.database.model.feed.ItemWithFeed
import mil.nga.giat.mage.data.repository.layer.LayerRepository
import mil.nga.giat.mage.data.repository.location.LocationRepository
import mil.nga.giat.mage.data.repository.observation.ObservationRepository
import mil.nga.giat.mage.glide.model.Avatar
import mil.nga.giat.mage.map.annotation.MapAnnotation
import mil.nga.giat.mage.map.preference.MapLayerPreferences
import mil.nga.giat.mage.network.Server
import mil.nga.giat.mage.network.gson.asStringOrNull
import mil.nga.giat.mage.observation.ObservationImportantState
import mil.nga.giat.mage.database.model.location.Location
import mil.nga.giat.mage.data.datasource.location.LocationLocalDataSource
import mil.nga.giat.mage.database.model.observation.Observation
import mil.nga.giat.mage.data.datasource.observation.ObservationLocalDataSource
import mil.nga.giat.mage.database.model.feature.StaticFeature
import mil.nga.giat.mage.data.datasource.event.EventLocalDataSource
import mil.nga.giat.mage.database.model.user.User
import mil.nga.giat.mage.data.datasource.user.UserLocalDataSource
import mil.nga.giat.mage.data.repository.settings.SettingsRepository
import mil.nga.giat.mage.database.model.settings.MapSearchType
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
    private val layerRepository: LayerRepository,
    private val userLocalDataSource: UserLocalDataSource,
    private val eventLocalDataSource: EventLocalDataSource,
    private val observationLocalDataSource: ObservationLocalDataSource,
    private val locationLocalDataSource: LocationLocalDataSource,
    settingsRepository: SettingsRepository,
    locationRepository: LocationRepository,
    observationRepository: ObservationRepository,
): ViewModel() {
    var dateFormat: DateFormat =
        DateFormatFactory.format("yyyy-MM-dd HH:mm zz", Locale.getDefault(), application)

    private val eventId = MutableLiveData<Long>()

    val showMapSearchButton = settingsRepository.observeMapSettings().map { mapSettings ->
        mapSettings.searchType != MapSearchType.NONE
    }.asLiveData()

    val observations = observationRepository.getObservations().transform { observations ->
        val states = eventLocalDataSource.currentEvent?.let { event ->
            observations.map { observation ->
                val observationForm = observation.forms.firstOrNull()
                val formDefinition = observationForm?.formId?.let { formId ->
                    eventLocalDataSource.getForm(formId)
                }

                MapAnnotation.fromObservation(
                    event = event,
                    observation = observation,
                    formDefinition = formDefinition,
                    observationForm = observationForm,
                    geometryType = observation.geometry.geometryType,
                    context = application
                )
            }
        } ?: emptyList()

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
        feedIds.switchMap { feedIds ->
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

    private val observationId = MutableLiveData<Long?>()
    val observationMap: LiveData<ObservationMapState?> = observationId.switchMap { id ->
        liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            if (id != null) {
                val observation = observationLocalDataSource.read(id)
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
        val currentUser = userLocalDataSource.readCurrentUser()
        val isFavorite = if (currentUser != null) {
            val favorite = observation.favoritesMap[currentUser.remoteId]
            favorite != null && favorite.isFavorite
        } else false

        val importantState = if (observation.important?.isImportant == true) {
            val importantUser: User? = try {
                observation.important?.userId?.let { userLocalDataSource.read(it) }
            } catch (ue: UserException) { null }

            ObservationImportantState(
                description = observation.important?.description,
                user = importantUser?.displayName
            )
        } else null

        var primary: String? = null
        var secondary: String? = null
        val observationForm = observation.forms.firstOrNull()
        val formDefinition = observationForm?.formId?.let {
            eventLocalDataSource.getForm(it)
        }

        if (observation.forms.isNotEmpty()) {
            primary = observationForm?.properties?.find { it.key == formDefinition?.primaryMapField }?.value as? String
            secondary = observationForm?.properties?.find { it.key == formDefinition?.secondaryMapField }?.value as? String
        }

        val event = eventLocalDataSource.currentEvent
        val iconFeature = MapAnnotation.fromObservation(
            event = event,
            observation = observation,
            formDefinition = formDefinition,
            observationForm = observationForm,
            geometryType = observation.geometry.geometryType,
            context = application
        )

        val user = userLocalDataSource.read(observation.userId)
        val title = "${user?.displayName} \u2022 ${dateFormat.format(observation.timestamp)}"

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
            val observation = observationLocalDataSource.read(observationMapState.id)
            try {
                userLocalDataSource.readCurrentUser()?.let { user ->
                    if (observationMapState.favorite) {
                        observationLocalDataSource.unfavoriteObservation(observation, user)
                    } else {
                        observationLocalDataSource.favoriteObservation(observation, user)
                    }
                }

                observationId.value = observation.id
            } catch (ignore: ObservationException) {}
        }
    }

    private val locationId = MutableLiveData<Long?>()
    val location = locationId.switchMap { id ->
        liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            if (id != null) {
                val location = locationLocalDataSource.read(id)
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
                val liveData = feedItemDao.item(id.feedId, id.itemId).map {
                    feedItemToState(it)
                }.asLiveData()
                emitSource(liveData)
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
        val user = userState?.let {
            locationLocalDataSource.read(it.id).user
        }

        _userPhone.value = user
    }

    private val _staticFeatureId = MutableLiveData<StaticFeatureId?>()
    val staticFeature: LiveData<StaticFeatureMapState?> = _staticFeatureId.switchMap { id ->
        liveData {
            if (id != null) {
                layerRepository.getStaticFeature(id.layerId, id.featureId)?.let {
                    emit(staticFeatureToState(it))
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