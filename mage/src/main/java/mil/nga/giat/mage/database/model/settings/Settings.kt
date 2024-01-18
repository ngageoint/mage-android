package mil.nga.giat.mage.database.model.settings

import androidx.room.*

@Entity(tableName = "settings")
data class Settings(
   @PrimaryKey
   @ColumnInfo(name = "id")
   val id: Int = 0,

   @Embedded(prefix = "map_")
   val mapSettings: MapSettings?
)