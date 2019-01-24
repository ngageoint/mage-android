package mil.nga.giat.mage.form

import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import com.google.gson.JsonObject
import dagger.android.support.DaggerAppCompatActivity
import kotlinx.android.synthetic.main.activity_form_defaults.*
import mil.nga.giat.mage.R
import mil.nga.giat.mage.sdk.datastore.user.Event
import mil.nga.giat.mage.sdk.datastore.user.EventHelper
import mil.nga.giat.mage.sdk.exceptions.EventException
import javax.inject.Inject

class FormDefaultActivity : DaggerAppCompatActivity() {

    companion object {
        private val LOG_NAME = FormDefaultActivity::class.java.name

        private const val EVENT_ID_EXTRA = "EVENT_ID_EXTRA"
        private const val FORM_ID_EXTRA = "FORM_ID_EXTRA"

        fun intent(context: Context, event: Event, form: Form): Intent {
            val intent = Intent(context, FormDefaultActivity::class.java)
            intent.putExtra(EVENT_ID_EXTRA, event.id)
            intent.putExtra(FORM_ID_EXTRA, form.id)
            return intent
        }
    }

    @Inject
    lateinit var context: Context

    private var event: Event? = null
    private var formJson: JsonObject? = null
    private lateinit var formModel: FormViewModel
    private lateinit var formPreferences: FormPreferences

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        require(intent.hasExtra(EVENT_ID_EXTRA)) {"EVENT_ID_EXTRA is required to launch FormDefaultActivity"}
        require(intent.hasExtra(FORM_ID_EXTRA)) {"FORM_ID_EXTRA is required to launch FormDefaultActivity"}

        setContentView(R.layout.activity_form_defaults)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        formModel = ViewModelProviders.of(this).get(FormViewModel::class.java)
        formModel.formMode = FormMode.EDIT

        val eventHelper: EventHelper = EventHelper.getInstance(context)
        try {
            event = eventHelper.read(intent.getLongExtra(EVENT_ID_EXTRA, 0))
            formJson = event?.formMap?.get(intent.getLongExtra(FORM_ID_EXTRA, 0))

            val form = Form.fromJson(formJson)
            formPreferences = FormPreferences(context, event!!, form.id)

            if (formModel.getForm().value == null) {
                formModel.setForm(form, formPreferences.getDefaults())
            }
        } catch(e: EventException) {
            Log.e(LOG_NAME, "Error reading event", e)
        }

        supportActionBar?.title = event?.name

        formModel.getForm().value?.let { form ->
            formName.text = form.name
            formDescription.visibility = if (form.description.isNullOrEmpty()) View.GONE else View.VISIBLE
            formDescription.text = form.description
        }

        saveButton.setOnClickListener{ saveDefaults() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.form_defaults_menu, menu)

        for (i in 0 until menu.size()) {
            menu.getItem(i).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM or MenuItem.SHOW_AS_ACTION_WITH_TEXT)
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.clear_defaults -> {
                clearDefaults()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveDefaults() {
        val invalidFields = (formFragment as FormFragment).editFields.filter {
            !it.validate(enforceRequired = false)
        }
        if (invalidFields.isNotEmpty()) return

        formModel.getForm().value?.let { form ->
            val transform : (FormField<Any>) -> Pair<String, FormField<Any>> = { it.name to it }
            val formMap = form.fields.associateTo(mutableMapOf(), transform)
            val defaultFormMap = Form.fromJson(formJson).fields.associateTo(mutableMapOf(), transform)

            if (formMap.equals(defaultFormMap)) {
                formPreferences.clearDefaults()
            } else {
                formPreferences.saveDefaults(form)
            }
        }

        finish()
    }

    private fun clearDefaults() {
        val form = Form.fromJson(formJson)
        formModel.setForm(form)
    }
}
