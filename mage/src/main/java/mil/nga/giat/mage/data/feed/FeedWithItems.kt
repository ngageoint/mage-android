package mil.nga.giat.mage.data.feed

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Relation

@Entity
data class FeedWithItems(
        @Embedded
        val feed: Feed,

        @Relation(parentColumn = "id", entityColumn = "feed_id")
        val items: List<FeedItem>
)