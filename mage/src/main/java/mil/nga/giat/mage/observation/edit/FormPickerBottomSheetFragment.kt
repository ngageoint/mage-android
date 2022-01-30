package mil.nga.giat.mage.observation.edit

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.JsonParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.view_form_picker_item.view.*
import mil.nga.giat.mage.R
import mil.nga.giat.mage.form.Form
import mil.nga.giat.mage.form.FormViewModel
import mil.nga.giat.mage.network.gson.asJsonObjectOrNull
import mil.nga.giat.mage.sdk.datastore.user.EventHelper

@AndroidEntryPoint
class FormPickerBottomSheetFragment: BottomSheetDialogFragment() {

  interface OnFormClickListener {
    fun onFormPicked(form: Form)
  }

  data class FormState(val form: Form, val disabled: Boolean)

  var formPickerListener: OnFormClickListener? = null

  protected lateinit var viewModel: FormViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    viewModel = ViewModelProvider(this).get(FormViewModel::class.java)
  }

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

    // TODO get from ViewModel
    val jsonForms = EventHelper.getInstance(context).currentEvent.forms

    val forms = jsonForms
       .asSequence()
       .map { form ->
         JsonParser.parseString(form.json).asJsonObjectOrNull()?.let { json ->
           Form.fromJson(json)
         }
       }
       .filterNotNull()
       .map { form ->
         val formMax = form.max
         val totalOfForm = viewModel.observationState.value?.forms?.value?.filter { it.definition.id == form.id }?.size ?: 0
         val disabled = formMax != null && totalOfForm <= formMax

         FormState(form, disabled)
       }
       .toList()

    recyclerView.adapter = FormAdapter(forms) { onForm(it.form) }
  }

  private fun onForm(form: Form) {
    dismiss()
    formPickerListener?.onFormPicked(form)
  }

  private class FormAdapter(private val forms: List<FormState>, private val onFormClicked: (FormState) -> Unit) : RecyclerView.Adapter<FormViewHolder>()  {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FormViewHolder {
      val view = LayoutInflater.from(parent.context).inflate(R.layout.view_form_picker_item, parent, false)
      return FormViewHolder(view) { position -> onFormClicked(forms[position]) }
    }

    override fun onBindViewHolder(holder: FormViewHolder, position: Int) {
      val formState = forms[position]
      holder.bindForm(formState)
    }

    override fun getItemCount() = forms.size
  }

  private class FormViewHolder(val view: View, onFormClicked: (Int) -> Unit) : RecyclerView.ViewHolder(view) {
    init {
      view.setOnClickListener { onFormClicked(adapterPosition) }
    }

    fun bindForm(formState: FormState) {
      view.form_name.text = formState.form.name
      view.form_description.text = formState.form.description

      if (formState.disabled) {
        view.form_name.alpha = .38f
        view.form_description.alpha = .38f

        val color = formState.form.hexColor.replace("#", "#60")
        ImageViewCompat.setImageTintMode(view.form_icon, PorterDuff.Mode.SRC_ATOP)
        ImageViewCompat.setImageTintList(view.form_icon, ColorStateList.valueOf(Color.parseColor(color)))
      } else {
        view.form_name.alpha = .87f
        view.form_description.alpha = .60f

        // Lets add a tiny bit of transparency to soften things up.
        val color = formState.form.hexColor.replace("#", "#DE")
        ImageViewCompat.setImageTintMode(view.form_icon, PorterDuff.Mode.SRC_ATOP)
        ImageViewCompat.setImageTintList(view.form_icon, ColorStateList.valueOf(Color.parseColor(color)))
      }
    }
  }
}