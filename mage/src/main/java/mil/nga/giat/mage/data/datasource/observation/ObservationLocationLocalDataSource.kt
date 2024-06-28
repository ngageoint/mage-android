package mil.nga.giat.mage.data.datasource.observation

import android.content.Context
import android.net.Uri
import android.util.Log
import com.j256.ormlite.dao.Dao
import dagger.hilt.android.qualifiers.ApplicationContext
import mil.nga.giat.mage.data.datasource.event.EventLocalDataSource
import mil.nga.giat.mage.data.repository.observation.ObservationRepository
import mil.nga.giat.mage.database.dao.observationLocation.ObservationLocationDao
import mil.nga.giat.mage.database.model.event.Form
import mil.nga.giat.mage.database.model.observation.Observation
import mil.nga.giat.mage.database.model.observation.ObservationForm
import mil.nga.giat.mage.database.model.observation.ObservationLocation
import mil.nga.giat.mage.form.FieldType
import mil.nga.giat.mage.map.annotation.ObservationIconStyle
import mil.nga.giat.mage.sdk.utils.toGeometry
import mil.nga.sf.Point
import java.sql.SQLException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObservationLocationLocalDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: ObservationLocationDao,
    private val formDao: Dao<Form, Long>
) {

    fun observationLocations(eventId: String): List<ObservationLocation>
        = dao.observationLocations(eventId)

    fun observationLocations(
        observationId: Long,
        minLatitude: Double,
        maxLatitude: Double,
        minLongitude: Double,
        maxLongitude: Double
    ): List<ObservationLocation> = dao.observationLocations(
        observationId,
        minLatitude,
        maxLatitude,
        minLongitude,
        maxLongitude
    )

    fun observationLocations(
        eventId: String,
        minLatitude: Double,
        maxLatitude: Double,
        minLongitude: Double,
        maxLongitude: Double
    ): List<ObservationLocation> = dao.observationLocations(
        eventId,
        minLatitude,
        maxLatitude,
        minLongitude,
        maxLongitude
    )

    // TODO: couldn't inject EventLocalDataSource so i could search that due to circular injections
    fun getForm(formId: Long): Form? {
        var form: Form? = null
        try {
            val forms = formDao.queryBuilder()
                .where()
                .eq("formId", formId)
                .query()
            if (forms != null && forms.size > 0) {
                form = forms[0]
            }
        } catch (e: SQLException) {
            Log.e("ObservationLocationLocalDataSource", "Error pulling form with id: $formId", e)
        }
        return form
    }

    fun create(observation: Observation): List<ObservationLocation> {
        dao.deleteLocationsForObservation(observation.id)

        var order: Int = 0
        val observationLocations: MutableList<ObservationLocation> = arrayListOf()

        // sve the observations location
        var latitude = 0.0
        var longitude = 0.0
        var minLatitude = 0.0
        var maxLatitude = 0.0
        var minLongitude = 0.0
        var maxLongitude = .0

        observation.geometry.let { geometry ->
            geometry.centroid.let {
                latitude = it.y
                longitude = it.x
            }
            geometry.envelope.let {
                minLatitude = it.minY
                maxLatitude = it.maxY
                minLongitude = it.minX
                maxLongitude = it.maxX
            }
        }

        var observationLocation = ObservationLocation(
            eventRemoteId = observation.event.remoteId,
            observationId = observation.id,
            geometry = observation.geometry,
            latitude = latitude,
            longitude = longitude,
            maxLatitude =  maxLatitude,
            maxLongitude = maxLongitude,
            minLatitude = minLatitude,
            minLongitude = minLongitude
        )
        observationLocation.fieldName = "primary-observation-geometry"
        observation.forms.firstOrNull()?.let { primaryObservationForm ->
            observationLocation.formId = primaryObservationForm.formId
            getForm(primaryObservationForm.formId)?.let { eventForm ->
                val primaryField = eventForm.primaryMapField
                val secondaryMapField = eventForm.secondaryMapField
                observationLocation.primaryFieldText =
                    primaryObservationForm.properties?.find { it.key == primaryField }?.value as? String
                observationLocation.secondaryFieldText =
                    primaryObservationForm.properties?.find { it.key == secondaryMapField }?.value as? String
            }
        } ?: run {
            observationLocation.formId = -1
        }

        observationLocation.accuracy = observation.accuracy
        observationLocation.provider = observation.provider
        observationLocation.order = order
        order++
        observationLocations.add(observationLocation)

        // save each location field
        observation.forms.forEach { form ->
            val eventFormId = form.formId
            getForm(eventFormId)?.let { eventForm ->
                mil.nga.giat.mage.form.Form.fromJson(eventForm.json)?.let { eventForm ->
                    val fields = eventForm.fields
                    val geometryFields = fields.filter {
                        !it.archived && it.type == FieldType.GEOMETRY && form.propertiesMap[it.name]?.value != null
                    }.mapNotNull { formField ->
                        val value = form.propertiesMap[formField.name]?.value
                        val ol = if (value is ByteArray) {
                            mil.nga.giat.mage.observation.ObservationLocation(value.toGeometry())
                        } else {
                            value as? mil.nga.giat.mage.observation.ObservationLocation
                        }

                        latitude = 0.0
                        longitude = 0.0
                        minLatitude = 0.0
                        maxLatitude = 0.0
                        minLongitude = 0.0
                        maxLongitude = 0.0
                        val geometry = ol?.geometry ?: return@mapNotNull null

                        ol.let { observationLocation ->
                            observationLocation.geometry.let { geometry ->
                                geometry.centroid.let {
                                    latitude = it.y
                                    longitude = it.x
                                }
                                geometry.envelope.let {
                                    minLatitude = it.minY
                                    maxLatitude = it.maxY
                                    minLongitude = it.minX
                                    maxLongitude = it.maxX
                                }
                            }
                        }
                        observationLocation = ObservationLocation(
                            eventRemoteId = observation.event.remoteId,
                            observationId = observation.id,
                            geometry = geometry,
                            latitude = latitude,
                            longitude = longitude,
                            maxLatitude = maxLatitude,
                            maxLongitude = maxLongitude,
                            minLatitude = minLatitude,
                            minLongitude = minLongitude
                        )
                        val primaryField = eventForm.primaryMapField
                        val secondaryMapField = eventForm.secondaryMapField
                        observationLocation.primaryFieldText =
                            form.properties?.find { it.key == primaryField }?.value as? String
                        observationLocation.secondaryFieldText =
                            form.properties?.find { it.key == secondaryMapField }?.value as? String

                        observationLocation.fieldName = formField.name
                        observationLocation.formId = eventForm.id
                        observationLocation.order = order
                        order++
                        observationLocation
                    }
                    observationLocations.addAll(geometryFields)
                }
            }
        }
        observationLocations.forEach {
            dao.insert(it)
        }
        Log.i("Observation Location", "Inserted " + observationLocations.count() + " locations")
        return observationLocations
    }

    fun getIconUri(
        observationForm: ObservationForm?,
        formDefinition: Form,
        eventId: String,
        context: Context
    ): Uri? {
        var primary: String? = observationForm?.properties?.find { it.key == formDefinition?.primaryMapField }?.value as? String
        var secondary: String? = observationForm?.properties?.find { it.key == formDefinition?.secondaryMapField }?.value as? String
        return ObservationIconStyle.fromObservationProperties(
            eventId = eventId,
            formId = observationForm?.formId,
            primary = primary,
            secondary = secondary,
            context = context
        ).uri
    }
}