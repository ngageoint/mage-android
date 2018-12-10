package mil.nga.giat.mage.event

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SearchView
import android.util.Log
import android.view.View
import dagger.android.support.DaggerAppCompatActivity
import kotlinx.android.synthetic.main.activity_events.*
import mil.nga.giat.mage.LandingActivity
import mil.nga.giat.mage.MageApplication
import mil.nga.giat.mage.R
import mil.nga.giat.mage.login.LoginActivity
import mil.nga.giat.mage.sdk.datastore.user.Event
import mil.nga.giat.mage.sdk.datastore.user.EventHelper
import mil.nga.giat.mage.sdk.datastore.user.UserHelper
import mil.nga.giat.mage.sdk.fetch.EventServerFetch
import mil.nga.giat.mage.sdk.login.AccountDelegate
import mil.nga.giat.mage.sdk.login.RecentEventTask
import java.util.*
import javax.inject.Inject

class EventsActivity : DaggerAppCompatActivity(), EventsFetchFragment.EventsFetchListener {

    companion object {
        private val LOG_NAME = EventsActivity::class.java.name
        private const val EVENTS_FETCH_FRAGMENT_TAG = "EVENTS_FETCH_FRAGMENT_TAG"
    }

    @Inject
    lateinit var application: MageApplication

    private var events = emptyList<Event>()

    private lateinit var eventsFetchFragment: EventsFetchFragment

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_events)

        toolbar.title = "Welcome to MAGE"
        setSupportActionBar(toolbar)

        loadingStatus.visibility = View.VISIBLE

        searchView.isIconified = false
        searchView.setIconifiedByDefault(false)
        searchView.clearFocus()

        dismissButton.setOnClickListener {
            dismiss()
        }

        var fragment = supportFragmentManager.findFragmentByTag(EVENTS_FETCH_FRAGMENT_TAG) as EventsFetchFragment?
        // If the Fragment is non-null, then it is being retained over a configuration change.
        if (fragment == null) {
            fragment = EventsFetchFragment()
            supportFragmentManager.beginTransaction().add(fragment, EVENTS_FETCH_FRAGMENT_TAG).commit()
        }

        eventsFetchFragment = fragment
    }

    override fun onResume() {
        super.onResume()

        eventsFetchFragment.loadEvents()
    }

    override fun onEventsFetched(status: Boolean, error: Exception?) {
        try {
            events = EventHelper.getInstance(application).readAll()
        } catch (e: Exception) {
            Log.e(LOG_NAME, "Could not get events!")
        }

        if (events.isEmpty()) {
            Log.e(LOG_NAME, "User is part of no event!")
            application.onLogout(true, null)

            searchView.visibility = View.GONE
            loadingStatus.visibility = View.GONE
            dismissButton.visibility = View.VISIBLE
            noEventsText.visibility = if (status) View.VISIBLE else View.GONE
            noConnectionText.visibility = if (status) View.GONE else View.VISIBLE
        } else {
            eventsFetched()
        }
    }

    private fun eventsFetched() {
        if (events.size == 1) {
            chooseEvent(events[0])
        } else {
            val recentEvents = EventHelper.getInstance(application).recentEvents
            val eventListAdapter = EventListAdapter(events, recentEvents, EventListAdapter.OnEventClickListener { event -> chooseEvent(event) })

            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    return false
                }

                override fun onQueryTextChange(text: String): Boolean {
                    eventListAdapter.filter(text)
                    return true
                }
            })

            recyclerView.layoutManager = LinearLayoutManager(applicationContext)
            recyclerView.itemAnimator = DefaultItemAnimator()
            recyclerView.addItemDecoration(EventItemDecorator(applicationContext))
            recyclerView.adapter = eventListAdapter

            eventsAppBar.visibility = View.VISIBLE
            loadingStatus.visibility = View.GONE
        }
    }

    private fun dismiss() {
        startActivity(Intent(applicationContext, LoginActivity::class.java))
        finish()
    }

    private fun chooseEvent(event: Event) {
        eventsAppBar.visibility = View.GONE
        eventsContent.visibility = View.GONE
        loadingStatus.visibility = View.VISIBLE

        loadingText.text = "Loading ${event.name}"

        val eventFetch = EventServerFetch(applicationContext, event.remoteId)
        eventFetch.setEventFetchListener { _, _ -> finishEvent(event) }
        eventFetch.execute()
    }

    private fun finishEvent(event: Event) {
        // Send chosen event to the server
        val userRecentEventInfo = ArrayList<String>()
        userRecentEventInfo.add(event.remoteId)
        RecentEventTask(AccountDelegate {
            // No need to check if this failed
        }, applicationContext).execute(*userRecentEventInfo.toTypedArray())

        try {
            val userHelper = UserHelper.getInstance(applicationContext)
            val user = userHelper.readCurrentUser()
            userHelper.setCurrentEvent(user, event)
        } catch (e: Exception) {
            Log.e(LOG_NAME, "Could not set current event.")
        }

        // disable pushing locations
        if (!UserHelper.getInstance(applicationContext).isCurrentUserPartOfCurrentEvent) {
            val editor = PreferenceManager.getDefaultSharedPreferences(applicationContext).edit()
            editor.putBoolean(getString(R.string.reportLocationKey), false).apply()
        }

        // start up the landing activity!
        val launchIntent = Intent(applicationContext, LandingActivity::class.java)
        val extras = intent.extras
        if (extras != null) {
            launchIntent.putExtras(extras)
        }

        startActivity(launchIntent)
        finish()
    }
}
