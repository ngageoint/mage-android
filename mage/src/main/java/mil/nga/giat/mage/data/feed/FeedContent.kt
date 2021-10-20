package mil.nga.giat.mage.data.feed

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.mapbox.geojson.FeatureCollection
import mil.nga.giat.mage.data.gson.DateTimestampTypeAdapter
import mil.nga.giat.mage.data.gson.FeatureCollectionTypeAdapter
import mil.nga.sf.GeometryType


data class FeedContent(
        @SerializedName("items")
        @JsonAdapter(FeatureCollectionTypeAdapter::class)
        val items: List<FeedItem>
)