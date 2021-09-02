package mil.nga.giat.mage.form.edit.dialog

import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.dialog_select_field.*
import mil.nga.giat.mage.R
import org.apache.commons.lang3.StringUtils
import kotlin.collections.ArrayList

/**
 * Created by wnewman on 2/9/17.
 */

class SelectFieldDialog : DialogFragment() {

  interface SelectFieldDialogListener {
    fun onSelect(choices: List<String>)
  }

  private lateinit var adapter: ArrayAdapter<String>

  private var title: String? = null
  private var multi: Boolean = false

  private var choices: List<String> = ArrayList()
  private var selectedChoices: MutableList<String> = ArrayList()
  private var filteredChoices = ArrayList<String>()

  var listener: SelectFieldDialogListener? = null

  companion object {
    private const val TITLE_KEY = "TITLE_KEY"
    private const val MULTI_SELECT_KEY = "MULTI_SELECT_KEY"
    private const val VALUE_KEY = "VALUE_KEY"
    private const val CHOICES_KEY = "CHOICES_KEY"

    fun newInstance(title: String, choices: List<String>, value: String?): SelectFieldDialog {
      val fragment = SelectFieldDialog()
      val bundle = Bundle()
      bundle.putString(TITLE_KEY, title)
      bundle.putBoolean(MULTI_SELECT_KEY, false)
      bundle.putString(VALUE_KEY, value)
      bundle.putStringArray(CHOICES_KEY, choices.toTypedArray())

      fragment.setStyle(STYLE_NO_TITLE, 0)
      fragment.isCancelable = false
      fragment.arguments = bundle

      return fragment
    }

    fun newInstance(title: String, choices: List<String>, value: List<String>?): SelectFieldDialog {
      val fragment = SelectFieldDialog()
      val bundle = Bundle()
      bundle.putString(TITLE_KEY, title)
      bundle.putBoolean(MULTI_SELECT_KEY, true)
      bundle.putStringArray(VALUE_KEY, value?.toTypedArray())
      bundle.putStringArray(CHOICES_KEY, choices.toTypedArray())

      fragment.setStyle(STYLE_NO_TITLE, 0)
      fragment.isCancelable = false
      fragment.arguments = bundle

      return fragment
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setStyle(STYLE_NORMAL, R.style.AppTheme_Dialog_Fullscreen)

    title = arguments?.getString(TITLE_KEY, null)
    val choices = arguments?.getStringArray(CHOICES_KEY)
    if (choices != null) {
      this.choices = choices.asList()
    }

    multi = arguments?.getBoolean(MULTI_SELECT_KEY, false) ?: false
    if (multi) {
      selectedChoices = arguments?.getStringArray(VALUE_KEY)?.asList()?.toMutableList() ?: mutableListOf()
    } else {
      arguments?.getString(VALUE_KEY)?.let {
        selectedChoices.add(it)
      }
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.dialog_select_field, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp)
    toolbar.setNavigationOnClickListener { dismiss() }
    toolbar.title = title

    if (multi) {
      toolbar.inflateMenu(R.menu.edit_select_menu)
    }

    filteredChoices.addAll(choices)

    if (multi) {
      adapter = ArrayAdapter(
        requireContext(),
        android.R.layout.simple_list_item_multiple_choice,
        filteredChoices
      )
      listView.adapter = adapter
      listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE
    } else {
      adapter = ArrayAdapter(
        requireContext(),
        android.R.layout.simple_list_item_single_choice,
        filteredChoices
      )
      listView.adapter = adapter
      listView.choiceMode = ListView.CHOICE_MODE_SINGLE
    }

    if (selectedChoices.isNotEmpty()) {
      checkSelected()
      selected_content.visibility = View.VISIBLE
      selected_choices.text = getSelectedChoicesString(selectedChoices)
    }

    listView.setOnItemClickListener { adapterView, _, position, _ ->
      val selectedItem = adapterView.getItemAtPosition(position) as String

      if (multi) {
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
        save()
      }

      selected_content.visibility = if (selectedChoices.isEmpty()) View.INVISIBLE else View.VISIBLE
      selected_choices.text = getSelectedChoicesString(selectedChoices)
    }

    searchView.isIconified = false
    searchView.setIconifiedByDefault(false)
    searchView.clearFocus()
    searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
      override fun onQueryTextSubmit(text: String): Boolean {
        return false
      }

      override fun onQueryTextChange(text: String): Boolean {
        onSearchTextChanged(text)
        return true
      }
    })

    toolbar.setOnMenuItemClickListener { item ->
      when (item.itemId) {
        R.id.apply -> {
          save()
          true
        }
        else -> super.onOptionsItemSelected(item)
      }
    }

    clear.setOnClickListener { clearSelected() }
  }

  private fun getSelectedChoicesString(choices: List<String>): String {
    return choices.joinToString(", ")
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
    selected_content.visibility = View.INVISIBLE
    selectedChoices.clear()

    if (!multi) {
      save()
      dismiss()
    }
  }

  private fun save() {
    listener?.onSelect(selectedChoices)
    dismiss()
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

    if (multi) {
      listView.adapter =
        ArrayAdapter(context, android.R.layout.simple_list_item_multiple_choice, filteredChoices)
      listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE
    } else {
      listView.adapter =
        ArrayAdapter(context, android.R.layout.simple_list_item_single_choice, filteredChoices)
      listView.choiceMode = ListView.CHOICE_MODE_SINGLE
    }

    checkSelected()
  }
}
