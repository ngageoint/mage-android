package mil.nga.giat.mage.data.datasource.observation

import android.content.Context
import android.net.Uri
import mil.nga.giat.mage.database.model.observation.ObservationLocation
import mil.nga.giat.mage.map.annotation.ObservationIconStyle
import mil.nga.sf.Geometry
import java.io.File

data class ObservationMapItem(
    var id: Long,
    var observationId: Long,
    val geometry: Geometry?,
    val formId: Long?,
    val fieldName: String?,
    val eventId: String?,
    val accuracy: Float?,
    val provider: String?,
    val maxLatitude: Double?,
    val maxLongitude: Double?,
    val minLatitude: Double?,
    val minLongitude: Double?,
    val latitude: Double,
    val longitude: Double,
    val primaryFieldText: String?,
    val secondaryFieldText: String?
) {
    constructor(observationLocation: ObservationLocation): this(
        id = observationLocation.id,
        observationId = observationLocation.observationId,
        geometry = observationLocation.geometry,
        formId = observationLocation.formId,
        fieldName = observationLocation.fieldName,
        eventId = observationLocation.eventRemoteId,
        accuracy = observationLocation.accuracy,
        provider = observationLocation.provider,
        maxLatitude = observationLocation.maxLatitude,
        maxLongitude = observationLocation.maxLongitude,
        minLatitude = observationLocation.minLatitude,
        minLongitude = observationLocation.minLongitude,
        latitude = observationLocation.latitude,
        longitude = observationLocation.longitude,
        primaryFieldText = observationLocation.primaryFieldText,
        secondaryFieldText = observationLocation.secondaryFieldText
    )

    fun getIcon(context: Context): File? {
        if (eventId != null && formId != null) {
            return ObservationIconStyle.fromObservationPropertyValues(
                eventId = eventId,
                formId = formId,
                primaryFieldText = primaryFieldText,
                secondaryFieldText = secondaryFieldText,
                context = context
            )
        }
        return null
    }
}
