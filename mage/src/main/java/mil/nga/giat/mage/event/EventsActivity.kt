package mil.nga.giat.mage.event

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_events.*
import mil.nga.giat.mage.LandingActivity
import mil.nga.giat.mage.MageApplication
import mil.nga.giat.mage.R
import mil.nga.giat.mage.login.LoginActivity
import mil.nga.giat.mage.network.Resource
import mil.nga.giat.mage.sdk.datastore.user.Event
import mil.nga.giat.mage.sdk.datastore.user.EventHelper
import javax.inject.Inject

@AndroidEntryPoint
class EventsActivity : AppCompatActivity() {

    @Inject
    lateinit var application: MageApplication

    private lateinit var viewModel: EventViewModel

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_events)
        setSupportActionBar(toolbar)

        if (intent.getBooleanExtra(CLOSABLE_EXTRA, false)) {
            supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        toolbar.title = "Events"

        loadingStatus.visibility = View.VISIBLE

        searchView.isIconified = false
        searchView.setIconifiedByDefault(false)
        searchView.clearFocus()

        dismissButton.setOnClickListener { dismiss() }

        viewModel = ViewModelProvider(this).get(EventViewModel::class.java)
        viewModel.syncStatus.observe(this, { onEventSynced(it) })

        // TODO what to do if user is not in this event
        // TODO all this should be in view model, either pick event and go or load events
        var event: Event? = null
        try {
            val eventId = intent.getLongExtra(EVENT_ID_EXTRA, -1)
            event = EventHelper.getInstance(application).read(eventId)
        } catch (e: java.lang.Exception) {
            Log.e(LOG_NAME, "Could not read event", e)
        }

        if (event != null) {
            chooseEvent(event)
        } else {
            viewModel.events.observe(this, { onEvents(it)})
        }
    }

    private fun onEvents(resource: Resource<List<Event>>) {
        val events: Collection<Event>? = resource.data

        when {
            events?.size == 1 -> chooseEvent(events.first())
            events?.isNotEmpty() == true -> {
                val recentEvents = EventHelper.getInstance(application).recentEvents
                val eventListAdapter = EventListAdapter(events.toMutableList(), recentEvents) { event -> chooseEvent(event) }

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
            else -> {
                application.onLogout(true, null)

                searchView.visibility = View.GONE
                loadingStatus.visibility = View.GONE
                dismissButton.visibility = View.VISIBLE
                noEventsText.visibility = if (resource.status == Resource.Status.ERROR) View.VISIBLE else View.GONE
                noConnectionText.visibility = if (resource.status == Resource.Status.ERROR) View.GONE else View.VISIBLE
            }
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
        viewModel.syncEvent(event)
    }

    private fun onEventSynced(resource: Resource<out Event> ) {
        if (resource.data != null) {
            viewModel.setEvent(resource.data)
        }

        val launchIntent = Intent(applicationContext, LandingActivity::class.java)
        val extras = intent.extras
        if (extras != null) {
            launchIntent.putExtras(extras)
        }

        startActivity(launchIntent)
        finish()
    }

    companion object {
        private val LOG_NAME = EventsActivity::class.java.name

        @JvmStatic val EVENT_ID_EXTRA = "EVENT_ID_EXTRA"
        @JvmStatic val CLOSABLE_EXTRA = "CLOSABLE_EXTRA"
    }
}
