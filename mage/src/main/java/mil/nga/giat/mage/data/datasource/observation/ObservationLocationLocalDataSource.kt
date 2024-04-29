package mil.nga.giat.mage.data.datasource.observation

import com.j256.ormlite.dao.Dao
import mil.nga.giat.mage.database.dao.observationLocation.ObservationLocationDao
import mil.nga.giat.mage.database.model.event.Form
import mil.nga.giat.mage.database.model.observation.Observation
import mil.nga.giat.mage.database.model.observation.ObservationLocation
import mil.nga.giat.mage.form.FieldType
import mil.nga.giat.mage.sdk.utils.toGeometry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObservationLocationLocalDataSource @Inject constructor(
    private val dao: ObservationLocationDao,
    private val formDao: Dao<Form, Long>
) {

    fun create(observation: Observation): List<ObservationLocation> {
        var order: Int = 0
        var observationLocations: MutableList<ObservationLocation> = arrayListOf()

        // sve the observations location
        var latitude: Double = 0.0
        var longitude: Double = 0.0
        var minLatitude: Double = 0.0
        var maxLatitude: Double = 0.0
        var minLongitude: Double = 0.0
        var maxLongitude: Double = .0

        observation.geometry.let {
            it.centroid.let {
                latitude = it.y
                longitude = it.x
            }
            it.envelope.let {
                minLatitude = it.minY
                maxLatitude = it.maxY
                minLongitude = it.minX
                maxLongitude = it.maxX
            }
        }

        val observationLocation = ObservationLocation(
            eventRemoteId = observation.event.remoteId,
            geometry = observation.geometry,
            latitude = latitude,
            longitude = longitude,
            maxLatitude =  maxLatitude,
            maxLongitude = maxLongitude,
            minLatitude = minLatitude,
            minLongitude = minLongitude
        )
        observationLocation.fieldName = "primary-observation-geometry"
        observation.forms.firstOrNull()?.let {
            observationLocation.formId = it.formId
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
            val eventFormDB = formDao.queryForId(eventFormId)
            mil.nga.giat.mage.form.Form.fromJson(eventFormDB.json)?.let { eventForm ->
                val fields = eventForm.fields
                val geometryFields = fields.filter {
                    !it.archived && it.type == FieldType.GEOMETRY && form.propertiesMap.get(it.name)?.value != null
                }

                geometryFields.forEach { geometryField ->
                    val value = form.propertiesMap.get(geometryField.name)?.value
                    if (value is ByteArray) {
                        mil.nga.giat.mage.observation.ObservationLocation(value.toGeometry())
                    } else {
                        value as? mil.nga.giat.mage.observation.ObservationLocation
                    }
                    value?.let {

                    }
                }
            }
        }
//
//        // save each location field
//        if let forms = properties?[ObservationKey.forms.key] as? [[String: Any]]
//        {
//            for form in forms {
//                if let eventFormId = form[EventKey.formId.key] as? NSNumber,
//                let eventForm = event?.form(id: eventFormId)
//                {
//                    let geometryFields = eventForm.fields?.filter({ field in
//                            let archived = field[FieldKey.archived.key] as? Bool ?? false
//                        let type = field[FieldKey.type.key] as? String ?? ""
//                        return !archived && type == FieldType.geometry.key
//                    }) ?? [[:]]
//
//                    for geometryField in geometryFields {
//                        if let geometry = form[geometryField[FieldKey.name.key] as? String ?? ""] as? SFGeometry {
//                        if let observationLocation = NSEntityDescription.insertNewObject(forEntityName: "ObservationLocation", into: context) as? ObservationLocation {
//                        observationLocation.observation = self
//                        if let eventId = eventId {
//                            observationLocation.eventId = eventId.int64Value
//                        }
//                        observationLocation.fieldName = geometryField[FieldKey.name.key] as? String
//                        observationLocation.formId = eventFormId.int64Value
//                        observationLocation.geometryData = SFGeometryUtils.encode(geometry)
//                        if let centroid = geometry.centroid() {
//                            observationLocation.latitude = centroid.y.doubleValue
//                            observationLocation.longitude = centroid.x.doubleValue
//                        }
//                        if let envelope = geometry.envelope() {
//                            observationLocation.minLatitude = envelope.minY.doubleValue
//                            observationLocation.maxLatitude = envelope.maxY.doubleValue
//                            observationLocation.minLongitude = envelope.minX.doubleValue
//                            observationLocation.maxLongitude = envelope.maxX.doubleValue
//                        }
//                        observationLocation.order = order
//                        order += 1
//                        observationLocations.insert(observationLocation)
//                    }
//                    }
//                    }
//                }
//            }
//        }
//        self.locations = observationLocations
        return observationLocations
    }
}