package mil.nga.giat.mage.form

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import com.google.gson.JsonObject
import kotlinx.android.synthetic.main.activity_form_defaults.*
import mil.nga.giat.mage.R
import mil.nga.giat.mage.observation.form.FormMode
import mil.nga.giat.mage.observation.form.FormViewModel
import mil.nga.giat.mage.sdk.datastore.user.Event
import mil.nga.giat.mage.sdk.datastore.user.EventHelper
import mil.nga.giat.mage.sdk.exceptions.EventException

class FormDefaultActivity : AppCompatActivity() {

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

        val eventHelper: EventHelper = EventHelper.getInstance(applicationContext)
        try {
            event = eventHelper.read(intent.getLongExtra(EVENT_ID_EXTRA, 0))
            formJson = event?.formMap?.get(intent.getLongExtra(FORM_ID_EXTRA, 0))

            Form.fromJson(formJson)?.let {
                formPreferences = FormPreferences(applicationContext, event!!, it.id)

                // TODO multi-form
//                if (formModel.getForm().value == null) {
//                    formModel.setForm(it, formPreferences.getDefaults())
//                }
            }
        } catch(e: EventException) {
            Log.e(LOG_NAME, "Error reading event", e)
        }

        supportActionBar?.title = event?.name
        // TODO multi-form
//        formModel.getForm().value?.let { form ->
//            formName.text = form.name
//            formDescription.visibility = if (form.description.isNullOrEmpty()) View.GONE else View.VISIBLE
//            formDescription.text = form.description
//        }

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
        // TODO multi-form

//        val invalidFields = (formFragment as FormFragment).editFields.filter {
//            !it.validate(enforceRequired = false)
//        }
//        if (invalidFields.isNotEmpty()) return

//        formModel.getForm().value?.let { form ->
//            val transform : (FormField<Any>) -> Pair<String, FormField<Any>> = { it.name to it }
//            val formMap = form.fields.associateTo(mutableMapOf(), transform)
//
//            Form.fromJson(formJson)?.let {
//                val defaultFormMap = it.fields.associateTo(mutableMapOf(), transform)
//                if (formMap.equals(defaultFormMap)) {
//                    formPreferences.clearDefaults()
//                } else {
//                    formPreferences.saveDefaults(form)
//                }
//            }
//        }

        finish()
    }

    private fun clearDefaults() {
        // TODO multi-form
//        Form.fromJson(formJson)?.let {
//            formModel.setForm(it)
//        }
    }
}
