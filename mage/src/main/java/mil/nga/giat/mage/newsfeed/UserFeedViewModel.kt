package mil.nga.giat.mage.newsfeed

import android.app.Application
import android.content.SharedPreferences
import android.database.Cursor
import androidx.annotation.WorkerThread
import androidx.lifecycle.*
import com.j256.ormlite.android.AndroidDatabaseResults
import com.j256.ormlite.dao.CloseableIterator
import com.j256.ormlite.dao.Dao
import com.j256.ormlite.stmt.PreparedQuery
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mil.nga.giat.mage.R
import mil.nga.giat.mage.data.datasource.location.LocationLocalDataSource
import mil.nga.giat.mage.data.datasource.user.UserLocalDataSource
import mil.nga.giat.mage.data.repository.location.LocationRepository
import mil.nga.giat.mage.database.model.location.Location
import mil.nga.giat.mage.sdk.event.ILocationEventListener
import java.sql.SQLException
import java.util.*
import javax.inject.Inject

enum class RefreshState { LOADING, COMPLETE }
data class UserFeedState(val cursor: Cursor, val query: PreparedQuery<Location>, val filterText: String)

@HiltViewModel
class UserFeedViewModel @Inject constructor(
   private val application: Application,
   private val sharedPreferences: SharedPreferences,
   private val locationDao: Dao<Location, Long>,
   private val locationRepository: LocationRepository,
   private val locationLocalDataSource: LocationLocalDataSource,
   private val userLocalDataSource: UserLocalDataSource
): ViewModel() {

   private val _refreshState = MutableLiveData<RefreshState>()
   val refreshState: LiveData<RefreshState> = _refreshState

   private val filter = MutableLiveData<Int>()
   val userFeedState = filter.switchMap { id ->
      liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
         val dataset = query(id)
         emit(dataset)
      }
   }

   private val sharedPreferencesChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
      if (key == application.getString(R.string.activeLocationTimeFilterKey)) {
         filter.value = getTimeFilterId()
      }
   }

   private val locationListener = object : ILocationEventListener {
      override fun onLocationCreated(location: MutableCollection<Location>?) { requery() }
      override fun onLocationUpdated(location: Location?) { requery() }
      override fun onLocationDeleted(location: MutableCollection<Location>?) { requery() }
      override fun onError(error: Throwable?) {}
   }

   init {
      filter.value = getTimeFilterId()

      locationLocalDataSource.addListener(locationListener)
      sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferencesChangeListener)
   }

   override fun onCleared() {
      super.onCleared()

      locationLocalDataSource.removeListener(locationListener)
      sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferencesChangeListener)
   }

   fun refresh() {
      _refreshState.value = RefreshState.LOADING
      viewModelScope.launch {
         locationRepository.fetch()
         _refreshState.value = RefreshState.COMPLETE
      }
   }

   private fun requery() {
      filter.postValue(getTimeFilterId())
   }

   @Throws(SQLException::class)
   @WorkerThread
   private fun query(filterId: Int): UserFeedState {
      val calendar = Calendar.getInstance()
      var filterText = ""

      when (filterId) {
         application.resources.getInteger(R.integer.time_filter_last_month) -> {
            filterText = "Last Month"
            calendar.add(Calendar.MONTH, -1)
         }
         application.resources.getInteger(R.integer.time_filter_last_week) -> {
            filterText = "Last Week"
            calendar.add(Calendar.DAY_OF_MONTH, -7)
         }
         application.resources.getInteger(R.integer.time_filter_last_24_hours) -> {
            filterText = "Last 24 Hours"
            calendar.add(Calendar.HOUR, -24)
         }
         application.resources.getInteger(R.integer.time_filter_today) -> {
            filterText = "Since Midnight"
            calendar[Calendar.HOUR_OF_DAY] = 0
            calendar[Calendar.MINUTE] = 0
            calendar[Calendar.SECOND] = 0
            calendar[Calendar.MILLISECOND] = 0
         }
         application.resources.getInteger(R.integer.time_filter_custom) -> {
            val customFilterTimeUnit: String = getCustomTimeUnit()
            val customTimeNumber: Int = getCustomTimeNumber()
            filterText = "Last $customTimeNumber $customFilterTimeUnit"
            when (customFilterTimeUnit) {
               "Hours" -> calendar.add(Calendar.HOUR, -1 * customTimeNumber)
               "Days" -> calendar.add(Calendar.DAY_OF_MONTH, -1 * customTimeNumber)
               "Months" -> calendar.add(Calendar.MINUTE, -1 * customTimeNumber)
               else -> calendar.add(Calendar.MINUTE, -1 * customTimeNumber)
            }
         } else -> {
            calendar.time = Date(0)
         }
      }

      val currentUser = userLocalDataSource.readCurrentUser()

      val queryBuilder = locationDao.queryBuilder()
      val where = queryBuilder.where().gt("timestamp", calendar.time)
      if (currentUser != null) {
         where
            .and()
            .ne("user_id", currentUser.id)
            .and()
            .eq("event_id", currentUser.userLocal.currentEvent.id)
      }
      queryBuilder.orderBy("timestamp", false)

      val query = queryBuilder.prepare()
      val iterator: CloseableIterator<Location> = locationDao.iterator(query)
      val results = iterator.rawResults as AndroidDatabaseResults
      return UserFeedState(results.rawCursor, query, filterText)
   }

   private fun getTimeFilterId(): Int {
      return sharedPreferences.getInt(application.resources.getString(R.string.activeLocationTimeFilterKey), application.resources.getInteger(R.integer.time_filter_last_month))
   }

   private fun getCustomTimeUnit(): String {
      return sharedPreferences.getString(application.resources.getString(R.string.customLocationTimeUnitFilterKey), application.resources.getStringArray(R.array.timeUnitEntries)[0])!!
   }

   private fun getCustomTimeNumber(): Int {
      return sharedPreferences.getInt(application.resources.getString(R.string.customLocationTimeNumberFilterKey), 0)
   }

}