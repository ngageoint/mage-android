package mil.nga.giat.mage.feed.item

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint
import mil.nga.giat.mage.data.feed.FeedItem
import mil.nga.giat.mage.feed.FeedItemState

@AndroidEntryPoint
class FeedItemActivity: AppCompatActivity() {
    private lateinit var viewModel: FeedItemViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        require(intent.hasExtra(FEED_ID_EXTRA)) {"FEED_ID_EXTRA is required to launch FeedActivity"}
        require(intent.hasExtra(FEED_ITEM_ID_EXTRA)) {"FEED_ITEM_ID_EXTRA is required to launch FeedActivity"}
        val feedId = intent.getStringExtra(FEED_ID_EXTRA)!!
        val feedItemId = intent.getStringExtra(FEED_ITEM_ID_EXTRA)!!

        viewModel = ViewModelProvider(this).get(FeedItemViewModel::class.java)

        setContent {
            FeedItemScreen(
                feedItemLiveData = viewModel.getFeedItem(feedId, feedItemId),
                snackbar = viewModel.snackbar,
                onClose = { onBackPressed() },
                onLocationClick = { onLocationClick(it) }
            )
        }
    }

    private fun onLocationClick(location: String) {
        viewModel.copyToClipBoard(location)
    }

    companion object {
        private const val FEED_ID_EXTRA = "FEED_ID_EXTRA"
        private const val FEED_ITEM_ID_EXTRA = "FEED_ITEM_ID_EXTRA"

        fun intent(context: Context, item: FeedItem): Intent {
            return intent(context, item.feedId, item.id)
        }

        fun intent(context: Context, item: FeedItemState): Intent {
            return intent(context, item.feedId, item.id)
        }

        private fun intent(context: Context, feedId: String, itemId: String): Intent {
            val intent = Intent(context, FeedItemActivity::class.java)
            intent.putExtra(FEED_ID_EXTRA, feedId)
            intent.putExtra(FEED_ITEM_ID_EXTRA, itemId)
            return intent
        }
    }
}