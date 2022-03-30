package mil.nga.giat.mage.feed.item

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint
import mil.nga.giat.mage.R
import mil.nga.giat.mage.feed.FeedActivity
import mil.nga.giat.mage.feed.FeedItemState
import mil.nga.giat.mage.utils.googleMapsUri

@AndroidEntryPoint
class FeedItemActivity: AppCompatActivity() {
    private lateinit var viewModel: FeedItemViewModel

    enum class ResultType { NAVIGATE }

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
                onAction = { onAction(it) }
            )
        }
    }

    private fun onAction(action: FeedItemAction) {
        when(action) {
            is FeedItemAction.Location -> {
                onLocationClick(action.location)
            }
            is FeedItemAction.Directions -> {
                onDirections(action.feedItem)
            }
        }
    }

    private fun onLocationClick(location: String) {
        viewModel.copyToClipBoard(location)
    }

    private fun onDirections(feedItem: FeedItemState) {
        AlertDialog.Builder(this)
            .setTitle(application.resources.getString(R.string.navigation_choice_title))
            .setItems(R.array.navigationOptions) { _: DialogInterface?, which: Int ->
                when (which) {
                    0 -> {
                        val intent = Intent(Intent.ACTION_VIEW, feedItem.geometry?.googleMapsUri())
                        startActivity(intent)
                    }
                    1 -> {
                        val intent = Intent()
                        intent.putExtra(FEED_ID_EXTRA, feedItem.id.feedId)
                        intent.putExtra(FEED_ITEM_ID_EXTRA, feedItem.id.itemId)
                        intent.putExtra(FEED_ITEM_RESULT_TYPE, ResultType.NAVIGATE)
                        setResult(Activity.RESULT_OK, intent)
                        finish()
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    companion object {
        private const val FEED_ID_EXTRA = "FEED_ID_EXTRA"
        private const val FEED_ITEM_ID_EXTRA = "FEED_ITEM_ID_EXTRA"
        const val FEED_ITEM_RESULT_TYPE = "FEED_ITEM_RESULT_TYPE"

        fun intent(context: Context, item: FeedItemState): Intent {
            return intent(context, item.id.feedId, item.id.itemId)
        }

        fun intent(context: Context, feedId: String, itemId: String): Intent {
            val intent = Intent(context, FeedItemActivity::class.java)
            intent.putExtra(FEED_ID_EXTRA, feedId)
            intent.putExtra(FEED_ITEM_ID_EXTRA, itemId)
            return intent
        }
    }
}