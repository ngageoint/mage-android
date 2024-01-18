package mil.nga.giat.mage.database.model.feed

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import mil.nga.giat.mage.network.geojson.FeatureCollectionTypeAdapter


data class FeedContent(
        @SerializedName("items")
        @JsonAdapter(FeatureCollectionTypeAdapter::class)
        val items: List<FeedItem>
)