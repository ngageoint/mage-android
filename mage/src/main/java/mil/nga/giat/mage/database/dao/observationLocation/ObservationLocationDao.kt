package mil.nga.giat.mage.database.dao.observationLocation

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import mil.nga.giat.mage.database.model.feed.Feed
import mil.nga.giat.mage.database.model.observation.ObservationLocation

@Dao
interface ObservationLocationDao {
    @Transaction
    fun upsert(observationLocation: ObservationLocation): Boolean {
        val id = insert(observationLocation)
        if (id == -1L) {
            update(observationLocation)
        }

        return id != -1L;
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(observationLocation: ObservationLocation): Long

    @Update(onConflict = OnConflictStrategy.IGNORE)
    fun update(observationLocation: ObservationLocation)

    @Query("SELECT * FROM observation_location WHERE event_remote_id = :eventId")
    fun observationLocations(eventId: String): List<ObservationLocation>

    // this isn't quite right, should be min lat and max lat and min long and max long for the query parameters
    // but will work for now
    @Query("SELECT * FROM observation_location WHERE event_remote_id = :eventId AND latitude BETWEEN :minLatitude AND :maxLatitude AND longitude BETWEEN :minLongitude AND :maxLongitude")
    fun observationLocations(
        eventId: String,
        minLatitude: Double,
        maxLatitude: Double,
        minLongitude: Double,
        maxLongitude: Double
    ): List<ObservationLocation>

    // this isn't quite right, should be min lat and max lat and min long and max long for the query parameters
    // but will work for now
    @Query("SELECT * FROM observation_location WHERE observation_id = :observationId AND latitude BETWEEN :minLatitude AND :maxLatitude AND longitude BETWEEN :minLongitude AND :maxLongitude")
    fun observationLocations(
        observationId: Long,
        minLatitude: Double,
        maxLatitude: Double,
        minLongitude: Double,
        maxLongitude: Double
    ): List<ObservationLocation>

    @Query("DELETE FROM observation_location")
    fun destroy()
}