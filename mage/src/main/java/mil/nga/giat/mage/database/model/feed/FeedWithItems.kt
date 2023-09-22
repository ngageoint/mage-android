package mil.nga.giat.mage.database.model.feed

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Relation
import mil.nga.giat.mage.database.model.feed.Feed
import mil.nga.giat.mage.database.model.feed.FeedItem

@Entity
data class FeedWithItems(
   @Embedded
        val feed: Feed,

   @Relation(parentColumn = "id", entityColumn = "feed_id")
        val items: List<FeedItem>
)