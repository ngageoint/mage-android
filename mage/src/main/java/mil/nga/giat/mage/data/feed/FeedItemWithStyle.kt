package mil.nga.giat.mage.data.feed

import androidx.room.*
import androidx.room.ForeignKey.CASCADE
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import mil.nga.sf.Geometry

data class FeedItemWithStyle(
        @Embedded
        val feedItem: FeedItem,

        @Embedded
        val style: Style
)