package mil.nga.giat.mage.feed

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint
import mil.nga.giat.mage.data.feed.Feed
import mil.nga.giat.mage.feed.item.FeedItemActivity
import mil.nga.sf.util.GeometryUtils
import java.text.DecimalFormat

@AndroidEntryPoint
class FeedActivity: AppCompatActivity() {
    companion object {
        private const val FEED_ID_EXTRA = "FEED_ID_EXTRA"

        fun intent(context: Context, feed: Feed): Intent {
            val intent = Intent(context, FeedActivity::class.java)
            intent.putExtra(FEED_ID_EXTRA, feed.id)
            return intent
        }
    }

    lateinit var viewModel: FeedViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        require(intent.hasExtra(FEED_ID_EXTRA)) {"FEED_ID_EXTRA is required to launch FeedActivity"}
        val feedId = intent.getStringExtra(FEED_ID_EXTRA)!!

        viewModel = ViewModelProvider(this).get(FeedViewModel::class.java)
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
        val latLngFormat = DecimalFormat("###.#####")
        val point = GeometryUtils.getCentroid(item.geometry)
        val uriString = "http://maps.google.com/maps?daddr=${latLngFormat.format(point.y)},${latLngFormat.format(point.x)}"

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
        startActivity(intent)
    }
}