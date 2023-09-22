package mil.nga.giat.mage.feed

import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.switchMap
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import mil.nga.giat.mage.database.model.feed.Feed
import mil.nga.giat.mage.database.dao.feed.FeedDao
import mil.nga.giat.mage.database.dao.feed.FeedLocalDao
import mil.nga.giat.mage.data.repository.feed.FeedRepository
import mil.nga.giat.mage.data.datasource.event.EventLocalDataSource
import mil.nga.giat.mage.data.datasource.user.UserLocalDataSource
import mil.nga.giat.mage.sdk.event.IEventEventListener
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class FeedFetchService: LifecycleService() {

    companion object {
        private val LOG_NAME = FeedFetchService::class.java.name
        private const val MIN_FETCH_DELAY = 5L
    }

    @Inject lateinit var feedDao: FeedDao
    @Inject lateinit var feedLocalDao: FeedLocalDao
    @Inject lateinit var feedRepository: FeedRepository
    @Inject lateinit var userLocalDataSource: UserLocalDataSource
    @Inject lateinit var eventLocalDataSource: EventLocalDataSource

    private val eventId = MutableLiveData<String?>()
    private var polling = false
    private var pollJob: Job? = null

    override fun onCreate() {
        super.onCreate()

        userLocalDataSource.addListener(object: IEventEventListener {
            override fun onEventChanged() {
                lifecycleScope.launch(Dispatchers.Main) {
                    setEvent()
                }
            }

            override fun onError(error: Throwable?) {}
        })

        setEvent()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPoll()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        eventId.switchMap { eventId ->
            stopPoll()
            eventId?.let {
                feedDao.feedsLiveData(it)
            }
        }.observe(this) {
            if (!polling && it.isNotEmpty()) {
                stopPoll()
                startPoll()
            } else if (polling && it.isEmpty()) {
                stopPoll()
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)

        return null
    }

    private fun setEvent() {
        eventId.value = eventLocalDataSource.currentEvent?.remoteId
    }

    private fun startPoll() {
        polling = true
        pollJob = poll()
    }

    private fun stopPoll() {
        pollJob?.cancel()
        polling = false
    }

    private fun getFetchDelay(): Long {
        val now = Date().time
        val delay = eventId.value?.let { eventId ->
            feedLocalDao.getFeeds(eventId).map {
                val lastSync = it.local?.lastSync
                if (lastSync == null) {
                    MIN_FETCH_DELAY
                } else {
                    val elapsed = (now - lastSync)/1000
                    if (elapsed > it.feed.updateFrequency!!) MIN_FETCH_DELAY else it.feed.updateFrequency!! - elapsed
                }
            }.minOrNull() ?: MIN_FETCH_DELAY
        } ?: MIN_FETCH_DELAY

        Log.d(LOG_NAME, "Fetch feed items in $delay seconds.")

        return delay
    }

    private fun getNextFeed(): Feed? {
        val now = Date().time
        val feeds = eventId.value?.let { eventId ->
            feedLocalDao.getFeeds(eventId).sortedBy { it.local?.lastSync ?: 0 }
        }
        feeds?.forEach {
            val lastSync = it.local?.lastSync ?: return it.feed

            if ((now - lastSync) > (it.feed.updateFrequency!! * 1000)) {
                return it.feed
            }
        }

        return null
    }

    private fun poll(): Job {
        return lifecycleScope.launch(Dispatchers.IO) {
            while (isActive) {
                fetchFeed()
                delay(timeMillis = getFetchDelay() * 1000)
            }
        }
    }

    private suspend fun fetchFeed() {
        val feed = getNextFeed()
        if (feed != null) {
            Log.d(LOG_NAME, "Sync feed items for feed ${feed.title}")
            feedRepository.syncFeed(feed)
        }
    }
}
