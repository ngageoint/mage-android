package mil.nga.giat.mage.database.model.observation

import android.util.Log
import com.j256.ormlite.field.FieldType
import com.j256.ormlite.field.SqlType
import com.j256.ormlite.field.types.StringType
import org.json.JSONException
import org.json.JSONObject

class ObservationErrorPersister private constructor() : StringType(
   SqlType.STRING, arrayOf(ObservationErrorPersister::class.java)
) {
   override fun javaToSqlArg(fieldType: FieldType, javaObject: Any): Any {
      val error = javaObject as ObservationError
      return jsonFromError(error)
   }

   override fun sqlArgToJava(fieldType: FieldType, sqlArg: Any, columnPos: Int): Any {
      val json = sqlArg as String
      return errorFromJson(json)
   }

   private fun jsonFromError(error: ObservationError): String {
      val jsonObject = JSONObject()
      try {
         val statusCode = error.statusCode
         if (statusCode != null) {
            jsonObject.put(ERROR_STATUS_CODE_KEY, error.statusCode)
         }
         val message = error.message
         if (message != null) {
            jsonObject.put(ERROR_MESSAGE_KEY, error.message)
         }
         val description = error.description
         if (description != null) {
            jsonObject.put(ERROR_DESCRIPTION_KEY, error.description)
         }
      } catch (e: JSONException) {
         Log.i(LOG_NAME, "Error parsing observation error", e)
      }

      return jsonObject.toString()
   }

   private fun errorFromJson(errorJson: String): ObservationError {
      val observationError = ObservationError()
      try {
         val jsonObject = JSONObject(errorJson)
         if (jsonObject.has(ERROR_STATUS_CODE_KEY)) {
            observationError.statusCode = jsonObject.getInt(ERROR_STATUS_CODE_KEY)
         }
         if (jsonObject.has(ERROR_MESSAGE_KEY)) {
            observationError.message = jsonObject.getString(ERROR_MESSAGE_KEY)
         }
         if (jsonObject.has(ERROR_DESCRIPTION_KEY)) {
            observationError.description = jsonObject.getString(ERROR_DESCRIPTION_KEY)
         }
      } catch (e: JSONException) {
         Log.e(LOG_NAME, "Error parsing json into observation error class", e)
      }
      return observationError
   }

   companion object {
      private val LOG_NAME = ObservationErrorPersister::class.java.name

      private const val ERROR_STATUS_CODE_KEY = "ERROR_STATUS_CODE_KEY"
      private const val ERROR_MESSAGE_KEY = "ERROR_MESSAGE_KEY"
      private const val ERROR_DESCRIPTION_KEY = "ERROR_DESCRIPTION_KEY"

      @JvmStatic
      val singleton = ObservationErrorPersister()
   }
}