package mil.nga.giat.mage.feed.item

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import mil.nga.giat.mage.R

class FeedItemAdapter: ListAdapter<FeedItemProperty, FeedItemViewHolder>(itemCallback) {

    companion object {
        private val itemCallback = object : DiffUtil.ItemCallback<FeedItemProperty>() {
            override fun areItemsTheSame(oldItem: FeedItemProperty, newItem: FeedItemProperty): Boolean =
                    oldItem.key == newItem.key

            override fun areContentsTheSame(oldItem: FeedItemProperty, newItem: FeedItemProperty): Boolean =
                    oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return FeedItemViewHolder(view)
    }

    override fun getItemViewType(position: Int): Int = R.layout.view_feed_item_property

    override fun onBindViewHolder(holder: FeedItemViewHolder, position: Int) = holder.bind(getItem(position))
}