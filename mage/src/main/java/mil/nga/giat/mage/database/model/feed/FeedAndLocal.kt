package mil.nga.giat.mage.database.model.feed

import androidx.room.Embedded
import androidx.room.Relation

data class FeedAndLocal(
   @Embedded val feed: Feed,
   @Relation(
                parentColumn = "id",
                entityColumn = "feed_id"
        )
        val local: FeedLocal?
)