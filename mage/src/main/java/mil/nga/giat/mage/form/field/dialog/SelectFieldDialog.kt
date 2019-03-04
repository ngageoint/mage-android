package mil.nga.giat.mage.form.field.dialog

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.widget.SearchView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import kotlinx.android.synthetic.main.dialog_select_field.*
import mil.nga.giat.mage.R
import mil.nga.giat.mage.form.*
import org.apache.commons.lang3.StringUtils
import java.util.*

/**
 * Created by wnewman on 2/9/17.
 */

class SelectFieldDialog : DialogFragment() {

    private lateinit var field: ChoiceFormField<out Any>
    private lateinit var model: FormViewModel
    private lateinit var adapter: ArrayAdapter<String>

    private lateinit var fieldKey: String
    private var choices: List<String> = ArrayList()
    private var selectedChoices:MutableList<String> = ArrayList()
    private var filteredChoices = ArrayList<String>()

    companion object {
        private const val DEFAULT_TEXT = ""
        private val FORM_FIELD_KEY_EXTRA = "FORM_FIELD_KEY_EXTRA"

        fun newInstance(fieldKey: String): SelectFieldDialog {
            val fragment = SelectFieldDialog()
            val bundle = Bundle()
            bundle.putString(FORM_FIELD_KEY_EXTRA, fieldKey)

            fragment.setStyle(DialogFragment.STYLE_NO_TITLE, 0)
            fragment.isCancelable = false
            fragment.arguments = bundle

            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        require(arguments?.containsKey(FORM_FIELD_KEY_EXTRA) ?: false) {"FORM_FIELD_ID_EXTRA is required to launch DateFieldDialog"}

        model = activity?.run {
            ViewModelProviders.of(this).get(FormViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        fieldKey = arguments!!.getString(FORM_FIELD_KEY_EXTRA, null)
        field = model.getField(fieldKey) as ChoiceFormField<out Any>
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_select_field, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        searchToolbar.inflateMenu(R.menu.edit_select_menu);
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
            adapter = ArrayAdapter(context!!, android.R.layout.simple_list_item_multiple_choice, filteredChoices)
            listView.setAdapter(adapter)
            listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE)

            val multiChoiceField = (field as MultiChoiceFormField)
            multiChoiceField.value?.let { selectedChoices.addAll(it) }
        } else {
            adapter = ArrayAdapter(context!!, android.R.layout.simple_list_item_single_choice, filteredChoices)
            listView.setAdapter(adapter)
            listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE)

            val singleChoiceField = (field as SingleChoiceFormField)
            singleChoiceField.value?.let { selectedChoices.add(it) }
        }

        if (selectedChoices.isEmpty()) {
            selectedChoicesTextView.setText(DEFAULT_TEXT)
        } else {
            checkSelected()
            selectedChoicesTextView.setText(getSelectedChoicesString(selectedChoices))
        }

        listView.setOnItemClickListener { adapterView, itemView, position, id ->
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
                selectedChoicesTextView.setText(DEFAULT_TEXT)
            } else {
                selectedChoicesTextView.setText(getSelectedChoicesString(selectedChoices))
            }
        }

        searchView.setIconified(false)
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
                    (field as ChoiceFormField<String>).value = selectedChoices.getOrNull(0)
                }
                else -> {
                    (field as ChoiceFormField<List<String>>).value = selectedChoices
                }
            }

            dismiss()
        }

    }

    private fun getSelectedChoicesString(choices: List<String>): String {
        val template = "%d Selected - %s";
        return template.format(choices.size, choices.joinToString(" | "))
    }

    private fun checkSelected() {
        for (count in selectedChoices.indices) {
            val index = filteredChoices.indexOf(selectedChoices.get(count))
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
        filteredChoices.clear()

        for (position in choices.indices) {
            if (text.length <= choices.get(position).length) {
                val currentChoice = choices.get(position)
                if (StringUtils.containsIgnoreCase(currentChoice, text)) {
                    filteredChoices.add(currentChoice)
                }
            }
        }

        if (field.type == FieldType.MULTISELECTDROPDOWN) {
            listView.adapter = ArrayAdapter(context, android.R.layout.simple_list_item_multiple_choice, filteredChoices)
            listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE)
        } else {
            listView.adapter = ArrayAdapter(context, android.R.layout.simple_list_item_single_choice, filteredChoices)
            listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE)
        }

        checkSelected()
    }
}
