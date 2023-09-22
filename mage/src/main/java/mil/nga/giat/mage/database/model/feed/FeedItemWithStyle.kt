package mil.nga.giat.mage.database.model.feed

import androidx.room.*

data class FeedItemWithStyle(
   @Embedded
        val feedItem: FeedItem,

   @Embedded
        val mapStyle: MapStyle
)