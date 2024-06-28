package mil.nga.giat.mage.map.cache

import android.content.Context
import android.util.Log
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.TileOverlay
import mil.nga.geopackage.BoundingBox
import mil.nga.geopackage.GeoPackageFactory
import mil.nga.geopackage.attributes.AttributesRow
import mil.nga.geopackage.db.GeoPackageDataType
import mil.nga.geopackage.extension.related.ExtendedRelation
import mil.nga.geopackage.extension.related.RelatedTablesExtension
import mil.nga.geopackage.extension.related.RelationType
import mil.nga.geopackage.extension.schema.columns.DataColumnsDao
import mil.nga.geopackage.geom.GeoPackageGeometryData
import mil.nga.geopackage.map.MapUtils
import mil.nga.geopackage.map.geom.GoogleMapShape
import mil.nga.geopackage.map.geom.GoogleMapShapeConverter
import mil.nga.geopackage.map.tiles.overlay.FeatureOverlayQuery
import mil.nga.giat.mage.R
import mil.nga.giat.mage.map.GeoPackageAttribute
import mil.nga.giat.mage.map.GeoPackageFeatureMapState
import mil.nga.giat.mage.map.GeoPackageMediaProperty
import mil.nga.giat.mage.map.GeoPackageProperty
import mil.nga.giat.mage.utils.DateFormatFactory
import mil.nga.sf.Geometry
import mil.nga.sf.GeometryType
import mil.nga.sf.Point
import mil.nga.sf.proj.GeometryTransform
import java.util.*

/**
 * Constructor
 *
 * @param name         GeoPackage table name
 * @param geoPackage   GeoPackage name
 * @param cacheName    Cache name
 * @param count        count
 * @param minZoom      min zoom level
 * @param isIndexed    Indexed flag, true when the feature table is indexed
 * @param geometryType geometry type
 */
class GeoPackageFeatureTableCacheOverlay(
   name: String?, geoPackage: String?, cacheName: String?, count: Int, minZoom: Int,
   val isIndexed: Boolean,
   val geometryType: GeometryType,
) : GeoPackageTableCacheOverlay(
   name,
   geoPackage,
   cacheName,
   CacheOverlayType.GEOPACKAGE_FEATURE_TABLE,
   count,
   minZoom,
   MAX_ZOOM
) {

   /**
    * Mapping between feature ids and shapes
    */
   private val shapes: MutableMap<Long, GoogleMapShape> = HashMap()

   /**
    * Tile Overlay
    */
   var tileOverlay: TileOverlay? = null

   /**
    * Used to query the backing feature table
    */
   lateinit var featureOverlayQuery: FeatureOverlayQuery

   /**
    * Linked tile table cache overlays
    */
   private val linkedTiles: MutableList<GeoPackageTileTableCacheOverlay> = ArrayList()

   override fun removeFromMap() {
      shapes.values.forEach { it.remove() }
      shapes.clear()

      tileOverlay?.remove()
      tileOverlay = null

      linkedTiles.forEach { it.removeFromMap() }
   }

   override fun getIconImageResourceId(): Int {
      return R.drawable.ic_place_preference_24dp
   }

   override fun getInfo(): String {
      var minZoom = minZoom
      var maxZoom = maxZoom

      linkedTiles.forEach { tileTable ->
         minZoom = minZoom.coerceAtMost(tileTable.minZoom)
         maxZoom = maxZoom.coerceAtLeast(tileTable.maxZoom)
      }

      return "features: $count, zoom: $minZoom - $maxZoom"
   }

   override fun onMapClick(latLng: LatLng, mapView: MapView, map: GoogleMap): String? {
      return featureOverlayQuery.buildMapClickMessage(latLng, mapView, map)
   }

   /**
    * Add a shape
    *
    * @param id
    * @param shape
    */
   private fun addShape(id: Long, shape: GoogleMapShape) {
      shapes[id] = shape
   }

   /**
    * Remove a shape
    *
    * @param id
    * @return
    */
   private fun removeShape(id: Long): GoogleMapShape? {
      return shapes.remove(id)
   }

   /**
    * Add a shape to the map
    *
    * @param id
    * @param shape
    * @return added map shape
    */
   fun addShapeToMap(id: Long, shape: GoogleMapShape?, map: GoogleMap?): GoogleMapShape {
      val mapShape = GoogleMapShapeConverter.addShapeToMap(map, shape)
      addShape(id, mapShape)
      return mapShape
   }

   /**
    * Remove a shape from the map
    *
    * @param id
    * @return
    */
   fun removeShapeFromMap(id: Long): GoogleMapShape? {
      val shape = removeShape(id)
      shape?.remove()
      return shape
   }

   /**
    * Add a linked tile table cache overlay
    *
    * @param tileTable tile table cache overlay
    */
   fun addLinkedTileTable(tileTable: GeoPackageTileTableCacheOverlay) {
      linkedTiles.add(tileTable)
   }

   /**
    * Get the linked tile table cache overlays
    *
    * @return linked tile table cache overlays
    */
   val linkedTileTables: List<GeoPackageTileTableCacheOverlay>
      get() = linkedTiles

   override fun getFeaturesNearClick(
      latLng: LatLng,
      mapView: MapView,
      map: GoogleMap,
      context: Context
   ): List<GeoPackageFeatureMapState> {

      val zoom = MapUtils.getCurrentZoom(map)

      // Build a bounding box to represent the click location
      val boundingBox = MapUtils.buildClickBoundingBox(
         latLng,
         mapView,
         map,
         featureOverlayQuery.screenClickPercentage
      )
      return getFeatures(
         latLng = latLng,
         boundingBox = boundingBox,
         zoom = zoom,
         context = context
      )
   }

   override fun getFeatures(
      latLng: LatLng,
      boundingBox: BoundingBox,
      zoom: Float,
      context: Context
   ): List<GeoPackageFeatureMapState> {
      val features: MutableList<GeoPackageFeatureMapState> = ArrayList()
      val styles = featureOverlayQuery.featureTiles.featureTableStyles

      // Verify the features are indexed and we are getting information
      val maxFeaturesInfo = context.resources.getBoolean(R.bool.map_feature_overlay_max_features_info)
      val featuresInfo = context.resources.getBoolean(R.bool.map_feature_overlay_features_info)
      if (isIndexed && (maxFeaturesInfo || featuresInfo)) {
         if (featureOverlayQuery.isOnAtCurrentZoom(zoom.toDouble(), latLng)) {
            val tileFeatureCount = featureOverlayQuery.tileFeatureCount(latLng, zoom.toDouble())
            if (featureOverlayQuery.isMoreThanMaxFeatures(tileFeatureCount)) {
               features.add(GeoPackageFeatureMapState(
                  id = 0L,
                  geoPackage = geoPackage,
                  table = name,
                  title = "GeoPackage",
                  primary = "$name $tileFeatureCount Features",
                  secondary = "$tileFeatureCount features, zoom in for more detail"
               ))
            } else if (featuresInfo) {
               try {
                  // Query for the features near the click
                  val geoPackage = GeoPackageFactory.getManager(context).open(geoPackage)
                  val relatedTablesExtension = RelatedTablesExtension(geoPackage)
                  val relationsDao = relatedTablesExtension.extendedRelationsDao
                  val mediaTables: MutableList<ExtendedRelation> = ArrayList()
                  val attributeTables: MutableList<ExtendedRelation> = ArrayList()
                  if (relationsDao.isTableExists) {
                     mediaTables.addAll(relationsDao.getBaseTableRelations(name)
                        .filter { relation ->
                           relation.relationType == RelationType.MEDIA
                        }
                     )

                     attributeTables.addAll(relationsDao.getBaseTableRelations(name)
                        .filter { relation ->
                           relation.relationType == RelationType.ATTRIBUTES ||
                           relation.relationType == RelationType.SIMPLE_ATTRIBUTES
                        }
                     )
                  }

                  val results = featureOverlayQuery.queryFeatures(boundingBox)
                  for (featureRow in results) {
                     val attributeRows: MutableList<AttributesRow> = ArrayList()
                     val featureId = featureRow.id

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
                     val properties: MutableList<GeoPackageProperty> = ArrayList()
                     var geometry: Geometry? = Point(latLng.longitude, latLng.latitude)
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
                              featureOverlayQuery.featureTiles.featureDao.projection, 4326L
                           )
                           centroid = transform.transform(centroid)
                           geometry = centroid
                        }

                        if (value != null && featureRow.columns.getColumn(i).dataType != GeoPackageDataType.BLOB) {
                           properties.add(GeoPackageProperty(columnName, value))
                        }
                     }

                     val attributes: MutableList<GeoPackageAttribute> = ArrayList()
                     for (row in attributeRows) {
                        val attributeProperties: MutableList<GeoPackageProperty> = ArrayList()
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

                     val featureStyle = styles?.getFeatureStyle(featureRow)
                     val style = featureStyle?.style

                     val icon = if (featureStyle?.hasIcon() == true) {
                        featureStyle.icon.dataBitmap
                     } else null

                     val title: String? = getDate(properties, context)

                     val primary = getValue(
                        listOf("name", "title", "primaryfield"),
                        properties
                     ) ?: "GeoPackage Feature"

                     val secondary = getValue(
                        listOf("subtitle", "secondaryfield", "variantfield"),
                        properties
                     ) ?: name

                     features.add(GeoPackageFeatureMapState(
                        id = featureId,
                        geoPackage = this.geoPackage,
                        table = name,
                        title = title,
                        primary = primary,
                        secondary = secondary,
                        geometry = geometry,
                        image = icon,
                        properties = properties,
                        attributes = attributes
                     ))
                  }
               } catch (e: Exception) {
                  Log.e("featureOverlayQuery", "error", e)
               }
            }
         }
      }
      return features
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

   companion object {
      /**
       * Max zoom for features
       */
      const val MAX_ZOOM = 21
   }
}