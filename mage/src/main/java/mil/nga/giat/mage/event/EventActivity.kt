package mil.nga.giat.mage.event

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import mil.nga.giat.mage.MageApplication
import mil.nga.giat.mage.databinding.ActivityEventBinding
import mil.nga.giat.mage.databinding.RecyclerFormListItemBinding
import mil.nga.giat.mage.form.Form
import mil.nga.giat.mage.form.defaults.FormDefaultActivity
import mil.nga.giat.mage.sdk.datastore.user.Event
import mil.nga.giat.mage.sdk.datastore.user.EventHelper
import mil.nga.giat.mage.sdk.exceptions.EventException
import javax.inject.Inject

@AndroidEntryPoint
class EventActivity : AppCompatActivity() {

    companion object {
        private val LOG_NAME = EventActivity::class.java.name

        const val EVENT_ID_EXTRA = "EVENT_ID_EXTRA"
    }

    private lateinit var binding: ActivityEventBinding
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    var event: Event? = null

    @Inject
    lateinit var application: MageApplication

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        require(intent.hasExtra(EVENT_ID_EXTRA)) {"EVENT_ID_EXTRA is required to launch EventActivity"}

        binding = ActivityEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val eventHelper: EventHelper = EventHelper.getInstance(applicationContext)
        val eventId =  intent.extras?.getLong(EVENT_ID_EXTRA)
        try {
            event = eventHelper.read(eventId)
        } catch(e: EventException) {
            Log.e(LOG_NAME, "Error reading event", e)
        }

        viewManager = LinearLayoutManager(this)


        val forms = event?.forms?.mapNotNull {
            Form.fromJson(it.json)
        } ?: emptyList()

        viewAdapter = FormAdapter(forms, { onFormClicked(it) })

        binding.recyclerView.apply {
            layoutManager = viewManager
            adapter = viewAdapter
            setHasFixedSize(true)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        binding.eventName.text = event?.name
        binding.eventDescription.text = event?.description
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            finish()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    private fun onFormClicked(form: Form) {
        startActivity(FormDefaultActivity.intent(applicationContext, event!!, form))
    }

    class FormViewHolder(val binding: RecyclerFormListItemBinding, val onClickListener: (Form) -> Unit) : RecyclerView.ViewHolder(binding.root) {
        fun bind(form: Form) = with(itemView) {
            binding.nameView.text = form.name
            itemView.setOnClickListener{ onClickListener(form) }
        }
    }

    inner class FormAdapter(forms: List<Form>, val onClickListener: (Form) -> Unit) : RecyclerView.Adapter<FormViewHolder>() {
        private val forms = forms.filterNot { it.archived }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FormViewHolder {
            val binding = RecyclerFormListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return FormViewHolder(binding, onClickListener)
        }

        override fun onBindViewHolder(holder: FormViewHolder, position: Int) {
            holder.bind(forms[position])
        }

        override fun getItemCount() = forms.count()
    }
}
