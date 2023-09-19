package mil.nga.giat.mage.database.model.event

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable

@DatabaseTable(tableName = "forms")
class Form {

   @DatabaseField(generatedId = true)
   var _id: Long? = null

   @DatabaseField(canBeNull = false)
   var formId: Long = 0

   @DatabaseField(canBeNull = false, foreign = true, columnName = COLUMN_NAME_EVENT_ID)
   lateinit var event: Event

   @DatabaseField
   var primaryMapField: String? = null

   @DatabaseField
   var secondaryMapField: String? = null

   @DatabaseField
   var primaryFeedField: String? = null

   @DatabaseField
   var secondaryFeedField: String? = null

   @DatabaseField
   var style: String? = null

   @DatabaseField(canBeNull = false)
   lateinit var json: String

   companion object {
      private const val COLUMN_NAME_EVENT_ID = "event_id"

      @JvmStatic
      fun getColumnNameEventId(): String = COLUMN_NAME_EVENT_ID
   }

}