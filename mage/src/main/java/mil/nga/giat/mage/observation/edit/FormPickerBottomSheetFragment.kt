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
import mil.nga.giat.mage.R
import mil.nga.giat.mage.databinding.FragmentFormPickerBottomSheetBinding
import mil.nga.giat.mage.databinding.ViewFormPickerItemBinding
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

  private lateinit var binding: FragmentFormPickerBottomSheetBinding
  private lateinit var viewModel: FormViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    viewModel = ViewModelProvider(this).get(FormViewModel::class.java)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = FragmentFormPickerBottomSheetBinding.inflate(inflater, container, false)
    return binding.root
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
       .filter { !it.archived }
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
      val binding = ViewFormPickerItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
      return FormViewHolder(binding) { position -> onFormClicked(forms[position]) }
    }

    override fun onBindViewHolder(holder: FormViewHolder, position: Int) {
      val formState = forms[position]
      holder.bindForm(formState)
    }

    override fun getItemCount() = forms.size
  }

  private class FormViewHolder(val binding: ViewFormPickerItemBinding, onFormClicked: (Int) -> Unit) : RecyclerView.ViewHolder(binding.root) {
    init {
      binding.root.setOnClickListener { onFormClicked(adapterPosition) }
    }

    fun bindForm(formState: FormState) {
      binding.formName.text = formState.form.name
      binding.formDescription.text = formState.form.description

      if (formState.disabled) {
        binding.formName.alpha = .38f
        binding.formDescription.alpha = .38f

        val color = formState.form.hexColor.replace("#", "#60")
        ImageViewCompat.setImageTintMode(binding.formIcon, PorterDuff.Mode.SRC_ATOP)
        ImageViewCompat.setImageTintList(binding.formIcon, ColorStateList.valueOf(Color.parseColor(color)))
      } else {
        binding.formName.alpha = .87f
        binding.formDescription.alpha = .60f

        // Lets add a tiny bit of transparency to soften things up.
        val color = formState.form.hexColor.replace("#", "#DE")
        ImageViewCompat.setImageTintMode(binding.formIcon, PorterDuff.Mode.SRC_ATOP)
        ImageViewCompat.setImageTintList(binding.formIcon, ColorStateList.valueOf(Color.parseColor(color)))
      }
    }
  }
}