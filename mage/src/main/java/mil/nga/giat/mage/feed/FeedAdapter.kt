package mil.nga.giat.mage.feed

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import mil.nga.giat.mage.R
import mil.nga.giat.mage.data.feed.Feed
import mil.nga.giat.mage.data.feed.FeedItem
import mil.nga.giat.mage.data.feed.ItemWithFeed

class FeedAdapter(private val feed: Feed, private val listener: (FeedItem) -> Unit): PagedListAdapter<FeedItem, FeedViewHolder>(itemCallback) {

    companion object {
        private val itemCallback = object : DiffUtil.ItemCallback<FeedItem>() {
            override fun areItemsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean =
                    oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean =
                    oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return FeedViewHolder(view)
    }

    override fun getItemViewType(position: Int): Int {
        return R.layout.feed_list_item
    }

    override fun onViewRecycled(holder: FeedViewHolder) {
        super.onViewRecycled(holder)
        holder.recycle()
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        val item = getItem(position)!!
        with(holder) {
            bind(ItemWithFeed(feed, item))
            itemView.setOnClickListener {
                listener.invoke(item)
            }
        }
    }
}