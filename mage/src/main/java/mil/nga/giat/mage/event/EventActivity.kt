package mil.nga.giat.mage.event

import android.content.Context
import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.DividerItemDecoration.VERTICAL
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import dagger.android.support.DaggerAppCompatActivity
import kotlinx.android.synthetic.main.activity_event.*
import kotlinx.android.synthetic.main.recycler_form_list_item.view.*
import mil.nga.giat.mage.MageApplication
import mil.nga.giat.mage.R
import mil.nga.giat.mage.form.FormDefaultActivity
import mil.nga.giat.mage.sdk.datastore.user.Event
import mil.nga.giat.mage.sdk.datastore.user.EventHelper
import mil.nga.giat.mage.sdk.exceptions.EventException
import javax.inject.Inject

class EventActivity : DaggerAppCompatActivity() {

    companion object {
        private val LOG_NAME = EventActivity::class.java.name

        public val EVENT_ID_EXTRA = "EVENT_ID_EXTRA"
    }

    @Inject
    lateinit var context: Context

    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    var event: Event? = null

    @Inject
    lateinit var application: MageApplication

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        require(intent.hasExtra(EVENT_ID_EXTRA), {"EVENT_ID_EXTRA is required to launch EventActivity"})

        setContentView(R.layout.activity_event)

        setSupportActionBar(toolbar);
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val eventHelper: EventHelper = EventHelper.getInstance(context)
        val eventId =  intent.extras.getLong(EVENT_ID_EXTRA)
        try {
            event = eventHelper.read(eventId)
        } catch(e: EventException) {
            Log.e(LOG_NAME, "Error reading event", e)
        }

        viewManager = LinearLayoutManager(this)
        viewAdapter = FormAdapter(event?.forms ?: JsonArray(), { onFormClicked(it) })

        recyclerView.apply {
            layoutManager = viewManager
            adapter = viewAdapter
            setHasFixedSize(true)
            addItemDecoration(DividerItemDecoration(getContext(), VERTICAL))
        }

        eventName.text = event?.name
        eventDescription.text = event?.description
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return if (item?.itemId == android.R.id.home) {
            finish()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    private fun onFormClicked(formJson: JsonObject) {
        startActivity(FormDefaultActivity.intent(context, event!!, formJson))
    }

    class FormViewHolder(val view: View, val onClickListener: (JsonObject) -> Unit) : RecyclerView.ViewHolder(view) {

        fun bind(formJson: JsonElement) = with(itemView) {
            formJson.asJsonObject?.let { form ->
                nameView.text = form.get("name")?.asString

                itemView.setOnClickListener{ onClickListener(form) }
            }
        }

    }

    inner class FormAdapter(private val forms: JsonArray, val onClickListener: (JsonObject) -> Unit) : RecyclerView.Adapter<FormViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FormViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.recycler_form_list_item, parent, false)
            return FormViewHolder(view, onClickListener)
        }

        override fun onBindViewHolder(holder: FormViewHolder, position: Int) {
            holder.bind(forms.get(position))
        }

        override fun getItemCount() = forms.size()
    }
}
