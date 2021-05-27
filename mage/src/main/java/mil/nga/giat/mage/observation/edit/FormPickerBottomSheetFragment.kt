package mil.nga.giat.mage.observation.edit

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.os.Bundle
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import androidx.core.widget.ImageViewCompat
import mil.nga.giat.mage.R
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.JsonObject
import kotlinx.android.synthetic.main.view_form_picker_item.view.*
import mil.nga.giat.mage.form.Form
import mil.nga.giat.mage.sdk.datastore.user.EventHelper

class FormPickerBottomSheetFragment: BottomSheetDialogFragment() {

  interface OnFormClickListener {
    fun onFormPicked(form: Form)
  }

  var formPickerListener: OnFormClickListener? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_form_picker_bottom_sheet, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    val recyclerView: RecyclerView = view.findViewById(R.id.recycler_view)
    recyclerView.layoutManager = LinearLayoutManager(context)

    // TODO get from viewmodel
    val jsonForms = EventHelper.getInstance(context).currentEvent.forms
    val forms = mutableListOf<Form>()
    for (jsonForm in jsonForms) {
      Form.fromJson(jsonForm as JsonObject)?.let { form ->
        forms.add(form)
      }
    }

    recyclerView.adapter = FormAdapter(forms) {
      dismiss()
      formPickerListener?.onFormPicked(it)
    }
  }

  private class FormAdapter(private val forms: List<Form>, private val onFormClicked: (Form) -> Unit) : RecyclerView.Adapter<FormViewHolder>()  {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FormViewHolder {
      val view = LayoutInflater.from(parent.context).inflate(R.layout.view_form_picker_item, parent, false)
      return FormViewHolder(view) { position -> onFormClicked(forms[position]) }
    }

    override fun onBindViewHolder(holder: FormViewHolder, position: Int) {
      val form = forms[position]
      holder.bindForm(form)
    }

    override fun getItemCount() = forms.size
  }

  private class FormViewHolder(val view: View, onFormClicked: (Int) -> Unit) : RecyclerView.ViewHolder(view) {
    init {
      view.setOnClickListener { onFormClicked(adapterPosition) }
    }

    fun bindForm(form: Form) {
      view.form_name.text = form.name
      view.form_description.text = form.description

      // Lets add a tiny bit of transparency to soften things up.
      val color = form.hexColor.replace("#", "#DE")
      ImageViewCompat.setImageTintMode(view.form_icon, PorterDuff.Mode.SRC_ATOP)
      ImageViewCompat.setImageTintList(view.form_icon, ColorStateList.valueOf(Color.parseColor(color)))
    }
  }
}