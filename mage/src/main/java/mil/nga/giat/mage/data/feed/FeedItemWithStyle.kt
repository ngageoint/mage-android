package mil.nga.giat.mage.data.feed

import androidx.room.*

data class FeedItemWithStyle(
        @Embedded
        val feedItem: FeedItem,

        @Embedded
        val mapStyle: MapStyle
)