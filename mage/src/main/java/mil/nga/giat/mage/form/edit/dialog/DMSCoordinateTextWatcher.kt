package mil.nga.giat.mage.form.edit.dialog

import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.widget.EditText
import mil.nga.giat.mage.coordinate.CoordinateType
import mil.nga.giat.mage.coordinate.MutableDMSLocation

class DMSCoordinateTextWatcher(
   private val editText: EditText,
   private val coordinateType: CoordinateType,
   val onSplitCoordinates: (Pair<String, String>) -> Unit
): TextWatcher {
   private var span: Any = Unit

   override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

   override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
      editText.text.setSpan(span, start, start + count, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
   }

   override fun afterTextChanged(s: Editable?) {
      val text = s?.toString() ?: return

      val start: Int = editText.text.getSpanStart(span)
      val end: Int = editText.text.getSpanEnd(span)
      val count = end - start

      val replacement = if (count > 0) {
         val replaced = text.subSequence(start, start + count)
         text.replaceRange(start until start + count, replaced).uppercase()
      } else ""

      if (replacement.isEmpty() || ("-" == text && start == 0 && count == 0)) {
         return
      }

      if ("." == replacement) {
         updateText()
         return
      }

      val coordinates = if (count > 1) splitCoordinates(text) else emptyList()
      if (coordinates.size == 2) {
         updateText()
         onSplitCoordinates(coordinates.zipWithNext().first())
      } else {
         val dms = parseDMS(text, addDirection = count > 1)

         val selection = if (dms.lastIndex > end) {
            val nextDigitIndex = dms.substring(start + 1).indexOfFirst { it.isDigit() }
            if (nextDigitIndex != -1) {
               start + nextDigitIndex + 1
            } else null
         } else null

         updateText(dms)
         selection?.let { editText.setSelection(it) }
      }
   }

   private fun updateText(text: String? = null) {
      editText.removeTextChangedListener(this)
      editText.text.clear()
      text?.let { editText.append(it) }
      editText.addTextChangedListener(this)
   }

   private fun parseDMS(text: String, addDirection: Boolean): String {
      if (text.isEmpty()) {
         return ""
      }

      val dms = MutableDMSLocation.parse(text, coordinateType, addDirection)

      val degrees = dms.degrees?.let { degrees ->
         "$degrees° "
      } ?: ""

      val minutes = dms.minutes?.let { minutes ->
         val padded = minutes.toString().padStart(2, '0')
         "${padded}\" "
      } ?: ""

      val seconds = dms.seconds?.let { seconds ->
         val padded = seconds.toString().padStart(2, '0')
         "${padded}\' "
      } ?: ""

      val direction = dms.direction ?: ""

      return "$degrees$minutes$seconds$direction"
   }

   private fun splitCoordinates(text: String): List<String> {
      val coordinates = mutableListOf<String>()

      val parsable = text.lines().joinToString().trim()

      // if there is a comma, split on that
      if (parsable.contains(',')) {
         return parsable.split(",").map { coordinate ->
            coordinate.trim()
         }
      }

      // Check if there are any direction letters
      val firstDirectionIndex = parsable.indexOfFirst { "NSEW".contains(it.uppercase()) }
      if (firstDirectionIndex != -1) {
         if (parsable.contains('-')) {
            // Split coordinates on dash
            return parsable.split("-").map { coordinate ->
               coordinate.trim()
            }
         } else {
            // No dash, split on the direction
            val lastDirectionIndex = parsable.indexOfLast { "NSEW".contains(it.uppercase()) }

            if (firstDirectionIndex == 0) {
               if (lastDirectionIndex != firstDirectionIndex) {
                  return listOf(
                     parsable.substring(0, lastDirectionIndex - 1),
                     parsable.substring(lastDirectionIndex)
                  )
               }
            } else if (lastDirectionIndex == parsable.lastIndex) {
               if (lastDirectionIndex != firstDirectionIndex) {
                  return listOf(
                     parsable.substring(0, firstDirectionIndex + 1),
                     parsable.substring(firstDirectionIndex + 1)
                  )
               }
            }
         }
      }

      // If there is one white space character split on that
      val parts = parsable.split(" ")
      if (parts.size == 2) {
         return parts.map { coordinate -> coordinate.trim() }
      }

      return coordinates
   }

}