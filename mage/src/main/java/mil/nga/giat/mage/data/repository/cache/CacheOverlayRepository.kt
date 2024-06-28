package mil.nga.giat.mage.data.repository.cache

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mil.nga.geopackage.BoundingBox
import mil.nga.geopackage.GeoPackageCache
import mil.nga.geopackage.GeoPackageFactory
import mil.nga.geopackage.tiles.TileBoundingBoxUtils
import mil.nga.giat.mage.data.datasource.layer.LayerLocalDataSource
import mil.nga.giat.mage.data.repository.event.EventRepository
import mil.nga.giat.mage.map.GeoPackageFeatureMapState
import mil.nga.giat.mage.map.cache.CacheOverlay
import mil.nga.giat.mage.map.cache.CacheOverlayFilter
import mil.nga.giat.mage.map.cache.CacheOverlayType
import mil.nga.giat.mage.map.cache.CacheProvider
import mil.nga.giat.mage.map.cache.GeoPackageCacheOverlay
import mil.nga.proj.ProjectionConstants
import mil.nga.proj.ProjectionFactory
import javax.inject.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import mil.nga.geopackage.attributes.AttributesRow
import mil.nga.geopackage.db.GeoPackageDataType
import mil.nga.geopackage.extension.nga.style.FeatureTableStyles
import mil.nga.geopackage.extension.related.ExtendedRelation
import mil.nga.geopackage.extension.related.RelatedTablesExtension
import mil.nga.geopackage.extension.related.RelationType
import mil.nga.geopackage.extension.schema.columns.DataColumnsDao
import mil.nga.geopackage.geom.GeoPackageGeometryData
import mil.nga.giat.mage.map.GeoPackageAttribute
import mil.nga.giat.mage.map.GeoPackageMediaProperty
import mil.nga.giat.mage.map.GeoPackageProperty
import mil.nga.giat.mage.utils.DateFormatFactory
import mil.nga.sf.Geometry
import mil.nga.sf.Point
import mil.nga.sf.proj.GeometryTransform
import java.util.Date
import java.util.Locale

@Serializable
data class GeoPackageFeatureKey(
    val geoPackageName: String,
    val featureId: Long = -1,
//    val layerName: String,
    val tableName: String,
    val maxFeaturesFound: Boolean = false,
    val featureCount: Int = 1) {

    constructor(featureItem: GeoPackageFeatureMapState): this(
        geoPackageName = featureItem.geoPackage,
        featureId = featureItem.id,
//        layerName = featureItem.layerName,
        tableName = featureItem.table
//        maxFeaturesFound = featureItem.maxFeaturesFound,
//        featureCount = featureItem.featureCount
    )

    fun toKey(): String {
        return Json.encodeToString(this)
    }

    companion object {
        fun fromKey(key: String): GeoPackageFeatureKey {
            return Json.decodeFromString<GeoPackageFeatureKey>(key)
        }
    }
}

class CacheOverlayRepository @Inject constructor(
    private val application: Application,
    private val eventRepository: EventRepository,
    private val layerLocalDataSource: LayerLocalDataSource,
    val cacheProvider: CacheProvider
): CacheProvider.OnCacheOverlayListener {
    private var geoPackageCache: GeoPackageCache = GeoPackageCache(GeoPackageFactory.getManager(application))
    private var _cacheOverlays = MutableStateFlow<Map<String, CacheOverlay?>>(HashMap())
    val cacheOverlays: StateFlow<Map<String, CacheOverlay?>> = _cacheOverlays.asStateFlow()

    private var _cacheBoundingBox = MutableStateFlow<BoundingBox?>(null)
    val cacheBoundingBox: StateFlow<BoundingBox?> = _cacheBoundingBox

    init {
        cacheProvider.registerCacheOverlayListener(this)
    }

//    fun mapClick(latLng: LatLng) {
//        val features = cacheOverlays.value.values.flatMap { overlay ->
//            overlay?.getFeaturesNearClick(latLng, binding.mapView, map, application) ?: emptyList()
//        }
//    }

    fun getFeature(key: GeoPackageFeatureKey): GeoPackageFeatureMapState? {
        var mapState: GeoPackageFeatureMapState? = null
        val geoPackage = geoPackageCache.getOrOpen(key.geoPackageName)
        val featureDao = geoPackage.getFeatureDao(key.tableName)
        if (featureDao != null) {
            val featureRow = featureDao.queryForIdRow(key.featureId)
            val attributeRows: MutableList<AttributesRow> = java.util.ArrayList()
            val styles = FeatureTableStyles(geoPackage, featureRow.table)
            val featureId = featureRow.id

            val relatedTablesExtension = RelatedTablesExtension(geoPackage)
            val relationsDao = relatedTablesExtension.extendedRelationsDao
            val mediaTables: MutableList<ExtendedRelation> = java.util.ArrayList()
            val attributeTables: MutableList<ExtendedRelation> = java.util.ArrayList()
            if (relationsDao.isTableExists) {
                mediaTables.addAll(relationsDao.getBaseTableRelations(key.tableName)
                    .filter { relation ->
                        relation.relationType == RelationType.MEDIA
                    }
                )

                attributeTables.addAll(relationsDao.getBaseTableRelations(key.tableName)
                    .filter { relation ->
                        relation.relationType == RelationType.ATTRIBUTES ||
                                relation.relationType == RelationType.SIMPLE_ATTRIBUTES
                    }
                )
            }

            for (relation in attributeTables) {
                val relatedAttributes = relatedTablesExtension.getMappingsForBase(
                    relation.mappingTableName,
                    featureId
                )
                val attributesDao = geoPackage.getAttributesDao(relation.relatedTableName)
                for (relatedAttribute in relatedAttributes) {
                    val row = attributesDao.queryForIdRow(relatedAttribute)
                    if (row != null) {
                        attributeRows.add(row)
                    }
                }
            }

            val dataColumnsDao = DataColumnsDao.create(geoPackage)
            val properties: MutableList<GeoPackageProperty> = java.util.ArrayList()
            var geometry: Geometry = Point(0.0, 0.0)
            val geometryColumn = featureRow.geometryColumnIndex
            for (i in 0 until featureRow.columnCount()) {
                val value = featureRow.getValue(i)
                var columnName = featureRow.getColumnName(i)
                if (dataColumnsDao.isTable) {
                    val dataColumn =
                        dataColumnsDao.getDataColumn(featureRow.table.tableName, columnName)
                    if (dataColumn != null) {
                        columnName = dataColumn.name
                    }
                }

                if (i == geometryColumn) {
                    val geometryData = value as GeoPackageGeometryData
                    var centroid = geometryData.geometry.centroid
                    val transform = GeometryTransform.create(
                        featureDao.projection, 4326L
                    )
                    centroid = transform.transform(centroid)
                    geometry = centroid
                }

                if (value != null && featureRow.columns.getColumn(i).dataType != GeoPackageDataType.BLOB) {
                    properties.add(GeoPackageProperty(columnName, value))
                }
            }

            val attributes: MutableList<GeoPackageAttribute> = java.util.ArrayList()
            for (row in attributeRows) {
                val attributeProperties: MutableList<GeoPackageProperty> = java.util.ArrayList()
                val attributeId = row.id
                for (relation in mediaTables) {
                    val relatedMedia = relatedTablesExtension.getMappingsForBase(
                        relation.mappingTableName,
                        attributeId
                    )

                    val mediaDao = relatedTablesExtension.getMediaDao(relation.relatedTableName)
                    val mediaRows = mediaDao.getRows(relatedMedia)
                    for (mediaRow in mediaRows) {
                        var name = "Media"
                        var columnIndex = mediaRow.columns.getColumnIndex("title", false)
                        if (columnIndex == null) {
                            columnIndex = mediaRow.columns.getColumnIndex("name", false)
                        }
                        if (columnIndex != null) {
                            name = mediaRow.getValue(columnIndex).toString()
                        }

                        val typeIndex = mediaRow.columns.getColumnIndex("content_type", false)
                        if (typeIndex != null) {
                            val contentType = mediaRow.getValue(typeIndex).toString()
                            attributeProperties.add(GeoPackageMediaProperty(name, mediaRow.data, mediaDao.tableName, mediaRow.id, contentType))
                        }
                    }
                }

                for (i in 0 until row.columnCount()) {
                    val value = row.getValue(i)
                    var columnName = row.getColumnName(i)
                    if (dataColumnsDao.isTable) {
                        val dataColumn = dataColumnsDao.getDataColumn(row.table.tableName, columnName)
                        if (dataColumn != null) {
                            columnName = dataColumn.name
                        }
                    }

                    if (value != null && row.columns.getColumn(i).dataType != GeoPackageDataType.BLOB) {
                        attributeProperties.add(GeoPackageProperty(columnName, value))
                    }
                }
                attributes.add(GeoPackageAttribute(attributeProperties))
            }

            val featureStyle = styles.getFeatureStyle(featureRow)
            val style = featureStyle?.style

            val icon = if (featureStyle?.hasIcon() == true) {
                featureStyle.icon.dataBitmap
            } else null

            val title: String? = getDate(properties, application)

            val primary = getValue(
                listOf("name", "title", "primaryfield"),
                properties
            ) ?: "GeoPackage Feature"

            val secondary = getValue(
                listOf("subtitle", "secondaryfield", "variantfield"),
                properties
            ) ?: key.tableName

            mapState = GeoPackageFeatureMapState(
                id = featureId,
                geoPackage = key.geoPackageName,
                table = key.tableName,
                title = title,
                primary = primary,
                secondary = secondary,
                geometry = geometry,
                image = icon,
                properties = properties,
                attributes = attributes
            )
        }
        return mapState
    }

    private fun getDate(properties: List<GeoPackageProperty>, context: Context): String? {
        val dateFormat = DateFormatFactory.format(
            "yyyy-MM-dd HH:mm zz",
            Locale.getDefault(),
            context
        )

        val keys = listOf("date", "timestamp")
        val property = properties.find { property ->
            keys.contains(property.key.lowercase())
        }

        return (property?.value as? Date)?.let { date ->
            dateFormat.format(date)
        }
    }

    private fun getValue(keys: List<String>, properties: List<GeoPackageProperty>): String? {
        val property = properties.find { property ->
            keys.contains(property.key.lowercase())
        }

        return property?.value as? String
    }

    fun getFeatureKeys(
        zoom: Float,
        minLatitude: Double,
        maxLatitude: Double,
        minLongitude: Double,
        maxLongitude: Double
    ): List<GeoPackageFeatureKey> {
        val boundingBox = BoundingBox(minLongitude, minLatitude, maxLongitude, maxLatitude)
        val centroid = boundingBox.centroid

        val features = ArrayList<GeoPackageFeatureMapState>()

        for (cacheOverlay in cacheOverlays.value.values.filterNotNull()) {
            if (cacheOverlay.isEnabled && cacheOverlay is GeoPackageCacheOverlay) {

                for (tableCacheOverlay in cacheOverlay.children) {
                    // Check if the table is enabled
                    if (tableCacheOverlay.isEnabled) {
                        features.addAll(
                            tableCacheOverlay?.getFeatures(
                                LatLng(
                                    centroid.y,
                                    centroid.x
                                ), boundingBox, zoom, application
                            ) ?: emptyList()
                        )
                    }
                }
            }
        }

        return features.map {
            GeoPackageFeatureKey(it)
        }
    }

    override fun onCacheOverlay(cacheOverlays: List<CacheOverlay>) {
        CoroutineScope(Dispatchers.IO).launch {
            handleCacheOverlays(cacheOverlays)
        }
    }

    private suspend fun handleCacheOverlays(cacheOverlays: List<CacheOverlay>) {
        // Add all overlays that are in the preferences
        val currentEvent = eventRepository.getCurrentEvent()
        val layers = layerLocalDataSource.readByEvent(currentEvent, "GeoPackage");

        val currentOverlays = _cacheOverlays.value.toMutableMap()

        val overlays = CacheOverlayFilter(application, layers).filter(cacheOverlays)

        // Track enabled cache overlays
        val enabledCacheOverlays: MutableMap<String, CacheOverlay?> = HashMap()

        // Track enabled GeoPackages
        val enabledGeoPackages: MutableSet<String> = HashSet()

        var anyChanges = false
        var newCacheBoundingBox: BoundingBox? = null

        for (cacheOverlay in overlays) {
            val currentOverlay = currentOverlays.remove(cacheOverlay.cacheName)
            if (currentOverlay != null) {
                // check if anything changed
                anyChanges = anyChanges || cacheOverlay.isAdded || cacheOverlay.isEnabled != currentOverlay.isEnabled
            } else {
                anyChanges = true
            }

            // If this cache overlay potentially replaced by a new version
            if (cacheOverlay.isAdded && cacheOverlay.type == CacheOverlayType.GEOPACKAGE) {
                geoPackageCache.close(cacheOverlay.name)
            }

            // The user has asked for this overlay so open the file and set it up
            if (cacheOverlay.isEnabled && cacheOverlay is GeoPackageCacheOverlay) {

                for (tableCacheOverlay in cacheOverlay.children) {
                    // Check if the table is enabled
                    if (tableCacheOverlay.isEnabled) {

                        // Get and open if needed the GeoPackage
                        val geoPackage = geoPackageCache.getOrOpen(cacheOverlay.name)
                        enabledGeoPackages.add(geoPackage.name)

                        if (cacheOverlay.isAdded) {
                            try {
                                val boundingBox = geoPackage.getBoundingBox(
                                    ProjectionFactory.getProjection(ProjectionConstants.EPSG_WORLD_GEODETIC_SYSTEM.toLong()),
                                    tableCacheOverlay.name
                                )
                                if (boundingBox != null) {
                                    newCacheBoundingBox = if (newCacheBoundingBox == null) {
                                        boundingBox
                                    } else {
                                        TileBoundingBoxUtils.union(
                                            newCacheBoundingBox,
                                            boundingBox
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(
                                    LOG_NAME,
                                    "Failed to retrieve GeoPackage Table bounding box. GeoPackage: "
                                            + geoPackage.name + ", Table: " + tableCacheOverlay.name,
                                    e
                                )
                            }
                        }
                    }
                }
            }
            cacheOverlay.isAdded = false
        }

        if (anyChanges || currentOverlays.isNotEmpty()) {
            _cacheOverlays.value = overlays.associateBy { it.cacheName }
            _cacheBoundingBox.value = newCacheBoundingBox
        }

        // Close GeoPackages no longer enabled.  The API takes the passed in GeoPackage names
        // and closes all of the non passed in GeoPackages.
        geoPackageCache.closeRetain(enabledGeoPackages)
    }

    companion object {
        private val LOG_NAME = CacheOverlayRepository::class.java.name
    }
}