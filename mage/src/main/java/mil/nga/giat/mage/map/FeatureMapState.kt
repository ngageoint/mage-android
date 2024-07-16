package mil.nga.giat.mage.map

import mil.nga.giat.mage.map.annotation.MapAnnotation
import mil.nga.giat.mage.observation.ObservationImportantState
import mil.nga.sf.Geometry

open class FeatureMapState<I>(
   val id: I,
   val title: String? = null,
   val primary: String? = null,
   val secondary: String? = null,
   val geometry: Geometry? = null,
   val image: Any? = null
)

class ObservationLocationMapState(
   id: Long,
   title: String,
   geometry: Geometry,
   primary: String?,
   secondary: String?,
   iconAnnotation: MapAnnotation<Long>,
   val observationId: Long,
   val isPrimary: Boolean,
   val favorite: Boolean,
   val importantState: ObservationImportantState? = null,
): FeatureMapState<Long>(id, title, primary, secondary, geometry, iconAnnotation)

class ObservationMapState(
   id: Long,
   title: String,
   geometry: Geometry,
   primary: String?,
   secondary: String?,
   iconAnnotation: MapAnnotation<Long>,
   val favorite: Boolean,
   val importantState: ObservationImportantState? = null,
): FeatureMapState<Long>(id, title, primary, secondary, geometry, iconAnnotation)

class UserMapState(
   id: Long,
   title: String? = null,
   primary: String? = null,
   secondary: String? = null,
   geometry: Geometry,
   image: Any? = null,
   val email: String? = null,
   val phone: String? = null
): FeatureMapState<Long>(id, title, primary, secondary, geometry, image)

class StaticFeatureMapState(
   id: Long,
   title: String? = null,
   primary: String? = null,
   secondary: String? = null,
   geometry: Geometry,
   image: Any? = null,
   val content: String? = null
): FeatureMapState<Long>(id, title, primary, secondary, geometry, image)

class GeoPackageFeatureMapState(
   id: Long,
   title: String? = null,
   primary: String? = null,
   secondary: String? = null,
   geometry: Geometry? = null,
   image: Any? = null,
   val geoPackage: String,
   val table: String,
   val properties: List<GeoPackageProperty> = emptyList(),
   val attributes: List<GeoPackageAttribute> = emptyList()
): FeatureMapState<Long>(id, title, primary, secondary, geometry, image)

data class GeoPackageAttribute(
   val properties: List<GeoPackageProperty>
)

open class GeoPackageProperty(
   val key: String,
   open val value: Any
)

class GeoPackageMediaProperty(
   key: String,
   override val value: ByteArray,
   val mediaTable: String,
   val mediaId: Long,
   val contentType: String
): GeoPackageProperty(key, value)