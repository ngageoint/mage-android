package mil.nga.giat.mage.database.model.feed

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Relation

@Entity
data class ItemWithFeed(
   @Relation(parentColumn = "item_feed_id", entityColumn = "id")
        val feed: Feed,

   @Embedded(prefix = "item_")
        val item: FeedItem
)