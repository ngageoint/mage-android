package mil.nga.giat.mage.database.dao.settings

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import mil.nga.giat.mage.database.model.settings.Settings

@Dao
interface SettingsDao {
    @Transaction
    fun upsert(settings: Settings): Boolean {
        val id = insert(settings)
        if (id == -1L) {
            update(settings)
        }

        return id != -1L;
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(settings: Settings): Long

    @Update(onConflict = OnConflictStrategy.IGNORE)
    fun update(settings: Settings)

    @Query("SELECT * FROM settings LIMIT 1")
    @RewriteQueriesToDropUnusedColumns
    fun observeSettings(): Flow<Settings?>

    @Query("SELECT * FROM settings LIMIT 1")
    suspend fun getSettings(): Settings?

    @Query("DELETE FROM settings")
    fun destroy()
}