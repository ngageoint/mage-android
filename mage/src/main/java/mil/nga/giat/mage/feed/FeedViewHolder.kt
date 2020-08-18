package mil.nga.giat.mage.feed

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.feed_list_item.view.*
import mil.nga.giat.mage.R
import mil.nga.giat.mage.data.feed.ItemWithFeed
import mil.nga.giat.mage.utils.DateFormatFactory
import java.util.*

class FeedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val dateFormat = DateFormatFactory.format("yyyy-MM-dd HH:mm zz", Locale.getDefault(), itemView.context)

    init {
        recycle()
    }

    fun bind(itemWithFeed: ItemWithFeed) {
        val feed = itemWithFeed.feed

        Glide.with(itemView.context)
            .load(feed.mapStyle?.iconUrl)
            .placeholder(R.drawable.default_marker)
            .fitCenter()
            .into(itemView.icon)

        itemView.overline.background = null
        itemView.primary.background = null
        itemView.secondary.background = null

        if (hasContent(itemWithFeed)) {
            val properties = itemWithFeed.item.properties!!.asJsonObject

            itemView.noContent.visibility  = View.GONE

            if (feed.itemTemporalProperty != null) {
                itemView.overline.visibility = View.VISIBLE
                properties.asJsonObject.get(feed.itemTemporalProperty)?.asLong?.let {
                    itemView.overline.text = dateFormat.format(it)
                }
            }

            if (feed.itemPrimaryProperty != null) {
                itemView.primary.visibility = View.VISIBLE
                itemView.primary.text = properties.asJsonObject.get(feed.itemPrimaryProperty)?.asString
            }

            if (feed.itemSecondaryProperty != null) {
                itemView.secondary.visibility = View.VISIBLE
                itemView.secondary.text = properties.asJsonObject.get(feed.itemSecondaryProperty)?.asString
            }
        }
    }

    private fun hasContent(itemWithFeed: ItemWithFeed): Boolean {
        return if (itemWithFeed.item.properties?.isJsonObject == true) {
            val feed = itemWithFeed.feed
            val properties = itemWithFeed.item.properties.asJsonObject

            (feed.itemTemporalProperty != null && properties.get(feed.itemTemporalProperty) != null) ||
            (feed.itemPrimaryProperty != null && properties.get(feed.itemPrimaryProperty) != null) ||
            (feed.itemSecondaryProperty != null && properties.get(feed.itemSecondaryProperty) != null)
        } else false
    }

    fun recycle() {
        val backgroundColor = ResourcesCompat.getColor(itemView.context.resources, R.color.md_grey_500, null)
        val drawable = InsetDrawable(ColorDrawable(backgroundColor), 0, 4, 0, 4)

        itemView.noContent.visibility  = View.VISIBLE

        itemView.overline.text = null
        itemView.overline.visibility = View.GONE
        itemView.primary.background = drawable

        itemView.primary.text = null
        itemView.primary.background = drawable

        itemView.secondary.text = null
        itemView.secondary.background = drawable
    }
}