package mil.nga.giat.mage.preferences.color

import android.content.Context
import android.graphics.Color
import androidx.preference.PreferenceDialogFragmentCompat
import android.view.LayoutInflater
import mil.nga.giat.mage.R
import android.text.TextWatcher
import android.text.Editable
import android.os.Bundle
import android.view.View
import android.widget.EditText
import java.lang.Exception

class ColorPreferenceDialog(
   private val preference: ColorPickerPreference
) : PreferenceDialogFragmentCompat(), View.OnClickListener {

   private lateinit var editText: EditText

   override fun onCreateDialogView(context: Context): View? {
      val inflater = LayoutInflater.from(context)
      return inflater.inflate(R.layout.dialog_color_picker, null)
   }

   override fun onBindDialogView(view: View) {
      super.onBindDialogView(view)
      editText = view.findViewById(R.id.hexText)
      editText.addTextChangedListener(object : TextWatcher {
         override fun afterTextChanged(s: Editable) {
            setColor(s.toString())
         }

         override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
         override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
      })

      editText.setText(preference.color)

      addClickListener(view, R.id.orange_tile)
      addClickListener(view, R.id.red_tile)
      addClickListener(view, R.id.blue_tile)
      addClickListener(view, R.id.green_tile)
      addClickListener(view, R.id.indigo_tile)
      addClickListener(view, R.id.orange_tile)
      addClickListener(view, R.id.pink_tile)
      addClickListener(view, R.id.purple_tile)
      addClickListener(view, R.id.red_tile)
      addClickListener(view, R.id.light_blue_tile)
      addClickListener(view, R.id.yellow_tile)
      addClickListener(view, R.id.light_gray_tile)
      addClickListener(view, R.id.dark_gray_tile)
      addClickListener(view, R.id.black_tile)
   }

   override fun onDialogClosed(positiveResult: Boolean) {
      if (positiveResult) {
         val color = editText.text.toString()
         try {
            Color.parseColor(color)
            if (preference.callChangeListener(color)) {
               preference.color = color
            }
         } catch (ignore: Exception) {}
      }
   }

   override fun onClick(v: View) {
      val color = v.backgroundTintList!!.defaultColor
      val hexColor = String.format("#%06X", 0xFFFFFF and color)
      editText.setText(hexColor)
      editText.compoundDrawables[0]?.setTint(color)
   }

   private fun addClickListener(parent: View, viewId: Int) {
      val view = parent.findViewById<View>(viewId)
      view.setOnClickListener(this)
   }

   private fun setColor(hexColor: String) {
      try {
         val color = Color.parseColor(hexColor)
         editText.compoundDrawablesRelative[0]?.setTint(color)
      } catch (ignore: Exception) { }
   }

   init {
      val b = Bundle()
      b.putString(ARG_KEY, preference.key)
      arguments = b
   }
}