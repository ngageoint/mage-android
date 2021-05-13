package mil.nga.giat.mage.form.field.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.dialog_select_field.*
import mil.nga.giat.mage.R
import mil.nga.giat.mage.form.*
import mil.nga.giat.mage.sdk.datastore.observation.ObservationProperty
import org.apache.commons.lang3.StringUtils
import java.io.Serializable
import java.util.*

/**
 * Created by wnewman on 2/9/17.
 */

class SelectFieldDialog : DialogFragment() {

    interface SelectFieldDialogListener {
        fun onDismiss()
    }

    private lateinit var model: FormViewModel
    private lateinit var adapter: ArrayAdapter<String>

    private var formId: Long = 0
    private lateinit var field: ChoiceFormField<out Any>
    private lateinit var fieldName: String
    private lateinit var fieldModel: FormViewModel.FieldModel
    private var choices: List<String> = ArrayList()
    private var selectedChoices:MutableList<String> = ArrayList()
    private var filteredChoices = ArrayList<String>()

    var listener: SelectFieldDialogListener? = null

    companion object {
        private const val DEFAULT_TEXT = ""
        private const val FIELD_ID_KEY_EXTRA = "FIELD_ID_KEY_EXTRA"
        private const val FORM_FIELD_NAME_KEY_EXTRA = "FORM_FIELD_NAME_KEY_EXTRA"

        fun newInstance(formId: Long, fieldName: String): SelectFieldDialog {
            val fragment = SelectFieldDialog()
            val bundle = Bundle()
            bundle.putLong(FIELD_ID_KEY_EXTRA, formId)
            bundle.putString(FORM_FIELD_NAME_KEY_EXTRA, fieldName)

            fragment.setStyle(STYLE_NO_TITLE, 0)
            fragment.isCancelable = false
            fragment.arguments = bundle

            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        require(arguments?.containsKey(FIELD_ID_KEY_EXTRA) ?: false) {"FIELD_ID_KEY_EXTRA is required to launch SelectFieldDialog"}
        require(arguments?.containsKey(FORM_FIELD_NAME_KEY_EXTRA) ?: false) {"FORM_FIELD_NAME_KEY_EXTRA is required to launch SelectFieldDialog"}

        model = activity?.run {
            ViewModelProviders.of(this).get(FormViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        formId = requireArguments().getLong(FIELD_ID_KEY_EXTRA, 0)
        fieldName = requireArguments().getString(FORM_FIELD_NAME_KEY_EXTRA, null)
        val form = model.getForms().value?.find { it.definition.id == formId }
        field = form?.definition?.fields?.find { it.name == fieldName } as ChoiceFormField<out Any>
        fieldModel = form.fieldModels.find { it.definition.name == fieldName } as FormViewModel.FieldModel
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_select_field, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        searchToolbar.inflateMenu(R.menu.edit_select_menu)
        searchToolbar.title = field.title
        searchToolbar.setOnMenuItemClickListener { item ->
            when(item.itemId) {
                R.id.clear_selection -> {
                    clearSelected()
                    true
                } else -> super.onOptionsItemSelected(item)
            }
        }

        choices = field.choices.map { it.title }
        filteredChoices.addAll(choices)

        if (field.type == FieldType.MULTISELECTDROPDOWN) {
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_multiple_choice, filteredChoices)
            listView.adapter = adapter
            listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE

            val multiChoiceField = (fieldModel.liveData.value as? List<*>)
            multiChoiceField?.map { it.toString() }?.let {
                selectedChoices.addAll(it)
            }
        } else {
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_single_choice, filteredChoices)
            listView.adapter = adapter
            listView.choiceMode = ListView.CHOICE_MODE_SINGLE

            val singleChoiceField = fieldModel.liveData.value as? String
            singleChoiceField?.let { selectedChoices.add(it) }
        }

        if (selectedChoices.isEmpty()) {
            selectedChoicesTextView.text = DEFAULT_TEXT
        } else {
            checkSelected()
            selectedChoicesTextView.text = getSelectedChoicesString(selectedChoices)
        }

        listView.setOnItemClickListener { adapterView, _, position, _ ->
            val selectedItem = adapterView.getItemAtPosition(position) as String

            if (field.type == FieldType.MULTISELECTDROPDOWN) {
                if (listView.isItemChecked(position)) {
                    if (!selectedChoices.contains(selectedItem)) {
                        selectedChoices.add(selectedItem)
                    }
                } else {
                    if (selectedChoices.contains(selectedItem)) {
                        selectedChoices.remove(selectedItem)
                    }
                }
            } else {
                selectedChoices.clear()
                selectedChoices.add(selectedItem)
            }

            if (selectedChoices.isEmpty()) {
                selectedChoicesTextView.text = DEFAULT_TEXT
            } else {
                selectedChoicesTextView.text = getSelectedChoicesString(selectedChoices)
            }
        }

        searchView.isIconified = false
        searchView.setIconifiedByDefault(false)
        searchView.clearFocus()
        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(text: String): Boolean {
                return false
            }

            override fun onQueryTextChange(text: String): Boolean {
                onSearchTextChanged(text)
                return true
            }
        })

        cancel.setOnClickListener { dismiss() }

        ok.setOnClickListener {
            when (field.type) {
                FieldType.DROPDOWN -> {
                    model.setFieldValue(0, fieldName, selectedChoices.getOrNull(0))
                }
                else -> {
                    model.setFieldValue(0, fieldName, selectedChoices)
                }
            }

            listener?.onDismiss()
            dismiss()
        }

    }

    private fun getSelectedChoicesString(choices: List<String>): String {
        val template = "%d Selected - %s"
        return template.format(choices.size, choices.joinToString(" | "))
    }

    private fun checkSelected() {
        for (count in selectedChoices.indices) {
            val index = filteredChoices.indexOf(selectedChoices[count])
            if (index != -1) {
                listView.setItemChecked(index, true)
            }
        }
    }

    private fun clearSelected() {
        listView.clearChoices()
        listView.invalidateViews()
        selectedChoicesTextView.text = DEFAULT_TEXT
        selectedChoices.clear()
    }

    private fun onSearchTextChanged(text: String) {
        val context = context ?: return

        filteredChoices.clear()

        for (position in choices.indices) {
            if (text.length <= choices[position].length) {
                val currentChoice = choices[position]
                if (StringUtils.containsIgnoreCase(currentChoice, text)) {
                    filteredChoices.add(currentChoice)
                }
            }
        }

        if (field.type == FieldType.MULTISELECTDROPDOWN) {
            listView.adapter = ArrayAdapter(context, android.R.layout.simple_list_item_multiple_choice, filteredChoices)
            listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        } else {
            listView.adapter = ArrayAdapter(context, android.R.layout.simple_list_item_single_choice, filteredChoices)
            listView.choiceMode = ListView.CHOICE_MODE_SINGLE
        }

        checkSelected()
    }
}
