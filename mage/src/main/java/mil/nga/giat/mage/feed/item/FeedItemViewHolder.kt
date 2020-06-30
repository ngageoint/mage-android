package mil.nga.giat.mage.feed.item

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.view_feed_item_property.view.*

class FeedItemViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
    fun bind(feedItemProperty: FeedItemProperty) {
        itemView.key.text = feedItemProperty.key
        itemView.value.text = feedItemProperty.value
    }
}