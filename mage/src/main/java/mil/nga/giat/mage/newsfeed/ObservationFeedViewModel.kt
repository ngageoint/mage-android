package mil.nga.giat.mage.newsfeed

import android.app.Application
import android.content.SharedPreferences
import android.database.Cursor
import android.util.Log
import androidx.lifecycle.*
import com.j256.ormlite.android.AndroidDatabaseResults
import com.j256.ormlite.dao.Dao
import com.j256.ormlite.stmt.PreparedQuery
import com.j256.ormlite.stmt.QueryBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mil.nga.giat.mage.R
import mil.nga.giat.mage.data.observation.ObservationRepository
import mil.nga.giat.mage.sdk.datastore.DaoStore
import mil.nga.giat.mage.sdk.datastore.observation.Observation
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper
import mil.nga.giat.mage.sdk.datastore.observation.State
import mil.nga.giat.mage.sdk.datastore.user.EventHelper
import mil.nga.giat.mage.sdk.datastore.user.UserHelper
import mil.nga.giat.mage.sdk.event.IObservationEventListener
import java.sql.SQLException
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ObservationFeedViewModel @Inject constructor(
   val application: Application,
   val preferences: SharedPreferences,
   private val observationRepository: ObservationRepository
): ViewModel() {

   enum class RefreshState { LOADING, COMPLETE }
   data class ObservationFeedState(val cursor: Cursor, val query: PreparedQuery<Observation>, val filterText: String)

   private val observationDao: Dao<Observation, Long> = DaoStore.getInstance(application).observationDao
   private val eventHelper = EventHelper.getInstance(application)
   private val userHelper = UserHelper.getInstance(application)
   private val observationHelper = ObservationHelper.getInstance(application)

   private var requeryTime: Long = 0
   private var refreshJob: Job? = null

   private val _refreshState = MutableLiveData<RefreshState>()
   val refreshState: LiveData<RefreshState> = _refreshState

   private val filter = MutableLiveData<Int>()
   val observationFeedState = filter.switchMap { id ->
      liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
         val feedState = query(id)
         scheduleRefresh(feedState.cursor)
         emit(feedState)
      }
   }

   private val sharedPreferencesChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
      if (key == application.getString(R.string.activeTimeFilterKey) ||
         key == application.getString(R.string.activeImportantFilterKey) ||
         key == application.getString(R.string.activeFavoritesFilterKey)) {
         filter.value = getTimeFilterId()
      }
   }

   private val observationListener = object : IObservationEventListener {
      override fun onObservationCreated(observations: MutableCollection<Observation>?, sendUserNotifcations: Boolean?) { requery() }
      override fun onObservationUpdated(observation: Observation?) { requery() }
      override fun onObservationDeleted(observation: Observation?) { requery() }
      override fun onError(error: Throwable?) {}
   }

   init {
      filter.value = getTimeFilterId()

      observationHelper.addListener(observationListener)
      preferences.registerOnSharedPreferenceChangeListener(sharedPreferencesChangeListener)
   }

   override fun onCleared() {
      super.onCleared()

      observationHelper.removeListener(observationListener)
      preferences.unregisterOnSharedPreferenceChangeListener(sharedPreferencesChangeListener)
   }

   fun refresh() {
      _refreshState.value = RefreshState.LOADING
      viewModelScope.launch {
         observationRepository.fetch(notify = false)
         _refreshState.value = RefreshState.COMPLETE
      }
   }

   private fun requery() {
      filter.postValue(getTimeFilterId())
   }

   @Throws(SQLException::class)
   private fun query(filterId: Int): ObservationFeedState {
      val currentUser = userHelper.readCurrentUser()

      val qb: QueryBuilder<Observation, Long> = observationDao.queryBuilder()
      val calendar = Calendar.getInstance()
      val filters = mutableListOf<String>()

      when(filterId) {
         application.resources.getInteger(R.integer.time_filter_last_month) -> {
            filters.add("Last Month")
            calendar.add(Calendar.MONTH, -1)
         }
         application.resources.getInteger(R.integer.time_filter_last_week) -> {
            filters.add("Last Week")
            calendar.add(Calendar.DAY_OF_MONTH, -7)
         }
         application.resources.getInteger(R.integer.time_filter_last_24_hours) -> {
            filters.add("Last 24 Hours")
            calendar.add(Calendar.HOUR, -24)
         }
         application.resources.getInteger(R.integer.time_filter_today) -> {
            filters.add("Since Midnight")
            calendar[Calendar.HOUR_OF_DAY] = 0
            calendar[Calendar.MINUTE] = 0
            calendar[Calendar.SECOND] = 0
            calendar[Calendar.MILLISECOND] = 0
         }
         application.resources.getInteger(R.integer.time_filter_custom) -> {
            val customFilterTimeUnit: String = getCustomTimeUnit()
            val customTimeNumber: Int = getCustomTimeNumber()
            filters.add("Last $customTimeNumber $customFilterTimeUnit")
            when (customFilterTimeUnit) {
               "Hours" -> calendar.add(Calendar.HOUR, -1 * customTimeNumber)
               "Days" -> calendar.add(Calendar.DAY_OF_MONTH, -1 * customTimeNumber)
               "Months" -> calendar.add(Calendar.MONTH, -1 * customTimeNumber)
               else -> calendar.add(Calendar.MINUTE, -1 * customTimeNumber)
            }
         }
         else -> calendar.time = Date(0)
      }

      requeryTime = calendar.timeInMillis

      qb.where()
         .ne("state", State.ARCHIVE)
         .and()
         .ge("timestamp", calendar.time)
         .and()
         .eq("event_id", eventHelper.currentEvent.id)

      val actionFilters: MutableList<String?> = ArrayList()

      val favorites: Boolean = preferences.getBoolean(application.resources.getString(R.string.activeFavoritesFilterKey), false)
      if (favorites && currentUser != null) {
         val observationFavoriteDao = DaoStore.getInstance(application).observationFavoriteDao
         val favoriteQb = observationFavoriteDao.queryBuilder()
         favoriteQb.where()
            .eq("user_id", currentUser.remoteId)
            .and()
            .eq("is_favorite", true)
         qb.join(favoriteQb)
         actionFilters.add("Favorites")
      }

      val important: Boolean = preferences.getBoolean(application.resources.getString(R.string.activeImportantFilterKey), false)
      if (important) {
         val observationImportantDao = DaoStore.getInstance(application).observationImportantDao
         val importantQb = observationImportantDao.queryBuilder()
         importantQb.where().eq("is_important", true)
         qb.join(importantQb)
         actionFilters.add("Important")
      }

      qb.orderBy("timestamp", false)

      if (actionFilters.isNotEmpty()) {
         filters.add(actionFilters.joinToString(" & "))
      }

      val query = qb.prepare()
      val iterator = observationDao.iterator(query)
      val results = iterator.rawResults as AndroidDatabaseResults
      return ObservationFeedState(results.rawCursor, query, filters.joinToString())
   }

   private fun scheduleRefresh(cursor: Cursor) {
      if (cursor.moveToLast()) {
         val oldestTime = cursor.getLong(cursor.getColumnIndexOrThrow("last_modified"))
         Log.d(LOG_NAME, "last modified is: " + cursor.getLong(cursor.getColumnIndexOrThrow("last_modified")))
         Log.d(LOG_NAME, "querying again in: " + (oldestTime - requeryTime) / 60000 + " minutes")

         refreshJob?.cancel()
         refreshJob = viewModelScope.launch {
            delay(oldestTime - requeryTime)
            filter.value = getTimeFilterId()
         }

         cursor.moveToFirst()
      }
   }

   private fun getTimeFilterId(): Int {
      return preferences.getInt(application.resources.getString(R.string.activeTimeFilterKey), application.resources.getInteger(R.integer.time_filter_last_month))
   }

   private fun getCustomTimeUnit(): String {
      return preferences.getString(application.resources.getString(R.string.customObservationTimeUnitFilterKey), application.resources.getStringArray(R.array.timeUnitEntries)[0])!!
   }

   private fun getCustomTimeNumber(): Int {
      return preferences.getInt(application.resources.getString(R.string.customObservationTimeNumberFilterKey), 0)
   }

   companion object {
      private val LOG_NAME = ObservationFeedViewModel::class.java.name
   }

}