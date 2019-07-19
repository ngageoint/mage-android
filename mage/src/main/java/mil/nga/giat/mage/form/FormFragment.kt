package mil.nga.giat.mage.form

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragement_form.*
import mil.nga.giat.mage.R
import mil.nga.giat.mage.form.field.*
import mil.nga.giat.mage.form.field.dialog.DateFieldDialog
import mil.nga.giat.mage.form.field.dialog.GeometryFieldDialog
import mil.nga.giat.mage.form.field.dialog.SelectFieldDialog
import mil.nga.giat.mage.observation.ObservationLocation
import java.util.*

class FormFragment : Fragment() {

    private lateinit var model: FormViewModel

    val editFields = ArrayList<Field<out Any>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        model = activity?.run {
            ViewModelProviders.of(this).get(FormViewModel::class.java)
        } ?: throw Exception("Invalid Activity")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
       return inflater.inflate(R.layout.fragement_form, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        model.getForm().observe(this, Observer<Form> { form ->
            form?.let { buildForm(it) }
        })
    }

    private fun buildForm(form: Form) {
        forms.removeAllViews()

        val fields = form.fields
                .filterNot { it.archived }
                .sortedBy { it.id }

        for (field in fields) {
            createField(context!!, field)?.let {
                forms.addView(it)
                editFields.add(it)
            }
        }
    }

    private fun createField(context: Context, field: FormField<in Any>): Field<out Any>? {
        return when (model.formMode) {
            FormMode.VIEW -> {
                return if (field.hasValue()) {
                    createViewField(context, field)
                } else null
            }
            FormMode.EDIT -> {
                createEditField(context, field)
            }
        }
    }

    private fun createViewField(context: Context, field: FormField<in Any>): Field<out Any>? {
        return when(field.type) {
            FieldType.TEXTFIELD -> {
                val view = ViewText(context)
                view.bind(field as FormField<String>)
                view
            }
            FieldType.TEXTAREA -> {
                val view = ViewText(context)
                view.bind(field as FormField<String>)
                view
            }
            FieldType.EMAIL -> {
                val view = ViewText(context)
                view.bind(field as FormField<String>)
                view
            }
            FieldType.PASSWORD -> {
                val view = ViewText(context)
                view.bind(field as FormField<String>)
                view
            }
            FieldType.NUMBERFIELD -> {
                val view = ViewNumber(context)
                view.bind(field as FormField<Number>)
                view
            }
            FieldType.DATE -> {
                val view = ViewDate(context)
                view.bind(field as FormField<Date>)
                view
            }
            FieldType.RADIO -> {
                val view = ViewText(context)
                view.bind(field as FormField<String>)
                view
            }
            FieldType.CHECKBOX -> {
                val view = ViewCheckbox(context)
                view.bind(field as FormField<Boolean>)
                view
            }
            FieldType.DROPDOWN -> {
                val view = ViewText(context)
                view.bind(field as FormField<String>)

                view
            }
            FieldType.MULTISELECTDROPDOWN -> {
                val view = ViewMultiselect(context)
                view.bind(field as ChoiceFormField<List<String>>)

                view
            }
            FieldType.GEOMETRY -> {
                val view = ViewGeometry(context)
                view.bind(field as FormField<ObservationLocation>)

                view
            }
        }
    }

    private fun createEditField(context: Context, field: FormField<in Any>): Field<out Any> {
        return when(field.type) {
            FieldType.TEXTFIELD -> {
                val view = EditText(context)
                view.bind(field as FormField<String>)
                view
            }
            FieldType.TEXTAREA -> {
                val view = EditTextarea(context)
                view.bind(field as FormField<String>)
                view
            }
            FieldType.EMAIL -> {
                val view = EmailField(context)
                view.bind(field as FormField<String>)
                view
            }
            FieldType.PASSWORD -> {
                val view = PasswordField(context)
                view.bind(field as FormField<String>)
                view
            }
            FieldType.NUMBERFIELD -> {
                val view = EditNumber(context)
                view.bind(field as FormField<Number>)
                view
            }
            FieldType.DATE -> {
                val view = EditDate(context)
                view.bind(field as FormField<Date>)
                view.setOnEditDateClickListener { onDateFieldClick(field) }
                view
            }
            FieldType.RADIO -> {
                val view = RadioField(context)
                view.bind(field as FormField<String>)
                view
            }
            FieldType.CHECKBOX -> {
                val view = EditCheckbox(context)
                view.bind(field as FormField<Boolean>)
                view
            }
            FieldType.DROPDOWN -> {
                val view = EditSelect(context)
                view.bind(field as ChoiceFormField<String>)
                view.setOnEditSelectClickListener {
                    onSelectFieldClick(field)
                }

                view
            }
            FieldType.MULTISELECTDROPDOWN -> {
                val view = EditMultiSelect(context)
                view.bind(field as ChoiceFormField<List<String>>)
                view.setOnEditSelectClickListener {
                    onSelectFieldClick(field)
                }

                view
            }
            FieldType.GEOMETRY -> {
                val view = EditGeometry(context)
                view.bind(field as FormField<ObservationLocation>)
                view.setOnEditGeometryClickListener {
                    onGeometryFieldClick(field)
                }

                view
            }
        }
    }

    private fun onDateFieldClick(field: FormField<*>) {
        val dialog = DateFieldDialog.newInstance(field.name)
        dialog.show(activity?.supportFragmentManager, "DIALOG_DATE_FIELD")
    }

    private fun onSelectFieldClick(field: FormField<*>) {
        val dialog = SelectFieldDialog.newInstance(field.name)
        dialog.show(activity?.supportFragmentManager, "DIALOG_SELECT_FIELD")
    }

    private fun onGeometryFieldClick(field: FormField<*>) {
        val dialog = GeometryFieldDialog.newInstance(field.name)
        dialog.show(activity?.supportFragmentManager, "DIALOG_GEOMETRY_FIELD")
    }
}