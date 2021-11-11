package mil.nga.giat.mage.data.feed

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Relation

@Entity
data class ItemWithFeed(
        @Relation(parentColumn = "feed_id", entityColumn = "id")
        val feed: Feed,

        @Embedded
        val item: FeedItem
)