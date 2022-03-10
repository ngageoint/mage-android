package mil.nga.giat.mage.feed

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import mil.nga.giat.mage.R
import mil.nga.giat.mage.data.feed.Feed
import mil.nga.giat.mage.feed.item.FeedItemActivity
import mil.nga.giat.mage.utils.googleMapsUri

@AndroidEntryPoint
class FeedActivity: AppCompatActivity() {
    enum class ResultType { NAVIGATE }

    private val viewModel: FeedViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        require(intent.hasExtra(FEED_ID_EXTRA)) {"FEED_ID_EXTRA is required to launch FeedActivity"}
        val feedId = intent.getStringExtra(FEED_ID_EXTRA)!!

        viewModel.setFeedId(feedId)

        setContent {
            FeedScreen(
                viewModel = viewModel,
                onClose = { onBackPressed() },
                onRefresh = { viewModel.refresh() },
                onItemAction = { onItemAction(it) }
            )
        }
    }

    private fun onItemAction(action: FeedItemAction) {
        when(action) {
            is FeedItemAction.Click -> onItemClick(action.item)
            is FeedItemAction.Location -> onLocationClick(action.text)
            is FeedItemAction.Directions -> onDirections(action.item)
        }
    }

    private fun onItemClick(item: FeedItemState) {
        val intent = FeedItemActivity.intent(this, item)
        startActivity(intent)
    }

    private fun onLocationClick(location: String) {
        viewModel.copyToClipBoard(location)
    }

    private fun onDirections(item: FeedItemState) {
        AlertDialog.Builder(this)
            .setTitle(application.resources.getString(R.string.navigation_choice_title))
            .setItems(R.array.navigationOptions) { _: DialogInterface?, which: Int ->
                when (which) {
                    0 -> {
                        val intent = Intent(Intent.ACTION_VIEW, item.geometry?.googleMapsUri())
                        startActivity(intent)
                    }
                    1 -> {
                        val data = Intent()
                        data.putExtra(FEED_RESULT_TYPE, ResultType.NAVIGATE)
                        data.putExtra(FEED_ID_EXTRA, item.id.feedId)
                        data.putExtra(FEED_ITEM_ID_EXTRA, item.id.itemId)
                        setResult(Activity.RESULT_OK, data)
                        finish()
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    companion object {
        const val FEED_RESULT_TYPE ="FEED_RESULT_TYPE"
        const val FEED_ID_EXTRA = "FEED_ID_EXTRA"
        const val FEED_ITEM_ID_EXTRA = "FEED_ITEM_ID_EXTRA"

        fun intent(context: Context, feed: Feed): Intent {
            val intent = Intent(context, FeedActivity::class.java)
            intent.putExtra(FEED_ID_EXTRA, feed.id)
            return intent
        }
    }
}