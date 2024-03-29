package mil.nga.giat.mage.form.edit.dialog

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import mil.nga.giat.mage.R
import mil.nga.giat.mage.form.FormState
import mil.nga.giat.mage.form.FormViewModel
import mil.nga.giat.mage.observation.ObservationState
import android.view.MotionEvent
import android.widget.ImageView
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.*
import mil.nga.giat.mage.databinding.DialogFormReorderBinding
import mil.nga.giat.mage.databinding.ViewFormReorderItemBinding
import java.util.*

class FormReorderDialog : DialogFragment() {

  interface FormReorderDialogListener {
    fun onReorder(forms: List<FormState>)
  }

  companion object {
    fun newInstance(): FormReorderDialog {
      val fragment = FormReorderDialog()

      fragment.setStyle(STYLE_NO_TITLE, 0)
      fragment.isCancelable = false

      return fragment
    }
  }

  private lateinit var binding: DialogFormReorderBinding
  var listener: FormReorderDialogListener? = null
  private lateinit var viewModel: FormViewModel
  private lateinit var itemTouchHelper: ItemTouchHelper

  private var forms: List<FormState>? = null

  val adapter = FormAdapter(emptyList(), object : FormAdapter.OnStartDragListener {
    override fun startDrag(viewHolder: RecyclerView.ViewHolder) {
      itemTouchHelper.startDrag(viewHolder)
    }
  })

  private val itemTouchHelperCallback = object: ItemTouchHelper.Callback() {
    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
      val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
      return makeMovementFlags(dragFlags, 0)
    }

    override fun onMove(
      recyclerView: RecyclerView,
      viewHolder: RecyclerView.ViewHolder,
      target: RecyclerView.ViewHolder
    ): Boolean {
      Collections.swap(forms, viewHolder.adapterPosition, target.adapterPosition)
      adapter.notifyItemMoved(viewHolder.adapterPosition, target.adapterPosition)
      return true
    }

    override fun isLongPressDragEnabled() = true
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setStyle(STYLE_NORMAL, R.style.AppTheme_Dialog_Fullscreen)

    viewModel = activity?.run {
      ViewModelProvider(this)[FormViewModel::class.java]

    } ?: throw Exception("Invalid Activity")
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = DialogFormReorderBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    binding.toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp)
    binding.toolbar.setNavigationOnClickListener { dismiss() }
    binding.toolbar.inflateMenu(R.menu.form_reorder_menu)

    binding.toolbar.setOnMenuItemClickListener { item ->
      when (item.itemId) {
        R.id.apply -> {
          apply()
          true
        }
        else -> super.onOptionsItemSelected(item)
      }
    }

    viewModel.observationState.observe(viewLifecycleOwner, { onObservationState(it) })

    binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
    binding.recyclerView.itemAnimator = DefaultItemAnimator()
    binding.recyclerView.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

    itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
    itemTouchHelper.attachToRecyclerView(binding.recyclerView)
    binding.recyclerView.adapter = adapter
  }

  private fun onObservationState(observationState: ObservationState) {
    val forms = observationState.forms.value.toMutableList()
    this.forms = forms
    adapter.forms = forms
    adapter.notifyItemRangeChanged(0, forms.size)
  }

  private fun apply() {
    forms?.let { listener?.onReorder(it) }
    dismiss()
  }

  @SuppressLint("ClickableViewAccessibility")
  class FormAdapter(var forms: List<FormState>, val listener: OnStartDragListener) : RecyclerView.Adapter<FormAdapter.ViewHolder>() {
    interface OnStartDragListener {
      fun startDrag(viewHolder: RecyclerView.ViewHolder)
    }

    class ViewHolder(binding: ViewFormReorderItemBinding) : RecyclerView.ViewHolder(binding.root) {
      val name: TextView = binding.name
      val primary: TextView = binding.primary
      val secondary: TextView = binding.secondary
      val formIcon: ImageView = binding.formIcon
      val dragIcon: ImageView = binding.dragIcon
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
      val binding = ViewFormReorderItemBinding.inflate(LayoutInflater.from(viewGroup.context), viewGroup, false)
      return ViewHolder(binding)
    }

    override fun getItemCount() = forms.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
      val formState: FormState = forms[position]

      // Adding some transparency to soften things up.
      val color = formState.definition.hexColor.replace("#", "#DE")
      ImageViewCompat.setImageTintMode(holder.formIcon, PorterDuff.Mode.SRC_ATOP)
      ImageViewCompat.setImageTintList(holder.formIcon, ColorStateList.valueOf(Color.parseColor(color)))

      holder.name.text = formState.definition.name

      val primaryState = formState.fields.find { it.definition.name == formState.definition.primaryFeedField }
      val primaryValue = if (primaryState?.hasValue() == true) {
        holder.primary.visibility = View.VISIBLE
        primaryState.answer?.serialize().toString()
      } else {
        holder.primary.visibility = View.GONE
        null
      }
      holder.primary.text = primaryValue

      val secondaryState = formState.fields.find { it.definition.name == formState.definition.secondaryFeedField }
      val secondaryValue = if (secondaryState?.hasValue() == true) {
        holder.secondary.visibility = View.VISIBLE
        secondaryState.answer?.serialize().toString()
      } else {
        holder.secondary.visibility = View.GONE
        null
      }
      holder.secondary.text = secondaryValue

      holder.dragIcon.setOnTouchListener { _, event ->
        if (event.action == MotionEvent.ACTION_DOWN) {
          listener.startDrag(holder)
        }
        false
      }
    }
  }
}
