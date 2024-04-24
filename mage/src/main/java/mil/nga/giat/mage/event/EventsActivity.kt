package mil.nga.giat.mage.event

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import mil.nga.giat.mage.LandingActivity
import mil.nga.giat.mage.MageApplication
import mil.nga.giat.mage.R
import mil.nga.giat.mage.databinding.ActivityEventsBinding
import mil.nga.giat.mage.login.LoginActivity
import mil.nga.giat.mage.network.Resource
import mil.nga.giat.mage.database.model.event.Event
import mil.nga.giat.mage.data.datasource.event.EventLocalDataSource
import javax.inject.Inject

@AndroidEntryPoint
class EventsActivity : AppCompatActivity() {

    @Inject lateinit var application: MageApplication
    @Inject lateinit var eventLocalDataSource: EventLocalDataSource

    private lateinit var binding: ActivityEventsBinding
    private lateinit var viewModel: EventViewModel

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEventsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        if (intent.getBooleanExtra(CLOSABLE_EXTRA, false)) {
            supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        binding.loadingStatus.visibility = View.VISIBLE

        binding.searchView.isIconified = false
        binding.searchView.setIconifiedByDefault(false)
        binding.searchView.clearFocus()

        binding.exit.setOnClickListener { dismiss() }

        viewModel = ViewModelProvider(this)[EventViewModel::class.java]
        viewModel.syncStatus.observe(this) { onEventSynced(it) }

        // TODO what to do if user is not in this event
        // TODO all this should be in view model, either pick event and go or load events
        val event = try {
            val eventId = intent.getLongExtra(EVENT_ID_EXTRA, -1)
            eventLocalDataSource.read(eventId)
        } catch (_: Exception) { null }

        if (event != null) {
            chooseEvent(event)
        } else {
            viewModel.events.observe(this) { onEvents(it) }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id: Int = item.itemId
        if (id == android.R.id.home) {
            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun onEvents(events: List<Event>) {
        when {
            events.size == 1 -> chooseEvent(events.first())
            events.isNotEmpty() -> {
                val recentEvents = eventLocalDataSource.getRecentEvents()
                val eventListAdapter = EventListAdapter(events.toMutableList(), recentEvents) { event -> chooseEvent(event) }

                binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String): Boolean {
                        return false
                    }

                    override fun onQueryTextChange(text: String): Boolean {
                        eventListAdapter.filter(text)
                        return true
                    }
                })

                binding.recyclerView.layoutManager = LinearLayoutManager(applicationContext)
                binding.recyclerView.itemAnimator = DefaultItemAnimator()
                binding.recyclerView.addItemDecoration(EventItemDecorator(applicationContext))
                binding.recyclerView.adapter = eventListAdapter

                binding.eventsAppBar.visibility = View.VISIBLE
                binding.loadingStatus.visibility = View.GONE
            }
            else -> {
                binding.searchView.visibility = View.GONE
                binding.loadingStatus.visibility = View.GONE
                binding.noContent.visibility = View.VISIBLE
            }
        }
    }

    private fun dismiss() {
        application.onLogout(true)
        startActivity(Intent(applicationContext, LoginActivity::class.java))
        finish()
    }

    private fun chooseEvent(event: Event) {
        binding.eventsAppBar.visibility = View.GONE
        binding.eventsContent.visibility = View.GONE
        binding.loadingStatus.visibility = View.VISIBLE

        binding.loadingText.text = "Loading ${event.name}"
        viewModel.syncEvent(event)
    }

    private fun onEventSynced(resource: Resource<out Event> ) {
        runBlocking {
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
    }

    companion object {
        @JvmStatic val EVENT_ID_EXTRA = "EVENT_ID_EXTRA"
        @JvmStatic val CLOSABLE_EXTRA = "CLOSABLE_EXTRA"
    }
}
