package mil.nga.giat.mage.feed

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import dagger.android.AndroidInjection
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mil.nga.giat.mage.dagger.module.ApplicationContext
import mil.nga.giat.mage.data.feed.Feed
import mil.nga.giat.mage.data.feed.FeedDao
import mil.nga.giat.mage.data.feed.FeedLocalDao
import mil.nga.giat.mage.data.feed.FeedRepository
import javax.inject.Inject
import mil.nga.giat.mage.sdk.datastore.user.EventHelper
import mil.nga.giat.mage.sdk.datastore.user.UserHelper
import mil.nga.giat.mage.sdk.event.IEventEventListener
import java.util.Date

class FeedFetchService : LifecycleService() {

    companion object {
        private val LOG_NAME = FeedFetchService::class.java.name
        private const val MIN_FETCH_DELAY = 5L
    }

    @Inject @field:ApplicationContext
    lateinit var context: Context

    @Inject
    lateinit var feedDao: FeedDao

    @Inject
    lateinit var feedLocalDao: FeedLocalDao

    @Inject
    lateinit var feedRepository: FeedRepository

    private val eventId = MutableLiveData<String>()
    private val feedHandler = Handler()
    private var feedTask: FeedTask? = null
    private var polling = false

    override fun onCreate() {
        super.onCreate()

        AndroidInjection.inject(this)

        UserHelper.getInstance(applicationContext).addListener(object: IEventEventListener {
            override fun onEventChanged() {
                setEvent()
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

        Transformations.switchMap(eventId) {
            feedDao.feedsLiveData(it)
        }.observe(this, Observer {
            if (!polling && it.isNotEmpty()) {
                stopPoll()
                startPoll()
            } else if (polling && it.isEmpty()) {
                stopPoll()
            }
        })

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)

        return null
    }

    private fun setEvent() {
        val event = EventHelper.getInstance(applicationContext).currentEvent
        eventId.value = event.remoteId
    }

    private fun startPoll() {
        polling = true
        scheduleFetch(0)
    }

    private fun stopPoll() {
        feedTask?.let { feedHandler.removeCallbacks(it) }
        polling = false
    }

    private fun scheduleFetch(delay: Long) {
        feedTask = FeedTask().apply {
            feedHandler.postDelayed(this, delay)
        }
    }

    private fun getFetchDelay(): Long {
        val now = Date().time
        val delay = feedLocalDao.getFeeds(eventId.value!!).map {
            val lastSync = it.local?.lastSync
            if (lastSync == null) {
                MIN_FETCH_DELAY
            } else {
                val elapsed = (now - lastSync)/1000
                if (elapsed > it.feed.updateFrequency!!) MIN_FETCH_DELAY else it.feed.updateFrequency!! - elapsed
            }
        }.min() ?: MIN_FETCH_DELAY

        Log.d(LOG_NAME, "Fetch feed items in $delay seconds.")

        return delay
    }

    private fun getNextFeed(): Feed? {
        val now = Date().time
        val feeds = feedLocalDao.getFeeds(eventId.value!!).sortedBy { it.local?.lastSync ?: 0 }
        feeds.forEach {
            val lastSync = it.local?.lastSync ?: return it.feed

            if ((now - lastSync) > (it.feed.updateFrequency!! * 1000)) {
                return it.feed
            }
        }

        return null
    }

    inner class FeedTask: Runnable {
        override fun run() {
            GlobalScope.launch {
                try {
                    val feed = getNextFeed()
                    if (feed != null) {
                        Log.d(LOG_NAME, "Sync feed items for feed ${feed.title}")
                        feedRepository.syncFeed(feed)
                    }
                } finally {
                    scheduleFetch(getFetchDelay() * 1000)
                }
            }
        }
    }
}
