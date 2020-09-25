package mil.nga.giat.mage.feed

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.android.support.DaggerAppCompatActivity
import kotlinx.android.synthetic.main.activity_feed.*
import mil.nga.giat.mage.R
import mil.nga.giat.mage.data.feed.Feed
import mil.nga.giat.mage.data.feed.FeedItem
import mil.nga.giat.mage.feed.item.FeedItemActivity
import javax.inject.Inject

class FeedActivity: DaggerAppCompatActivity() {
    companion object {
        private const val FEED_ID_EXTRA = "FEED_ID_EXTRA"

        fun intent(context: Context, feed: Feed): Intent {
            val intent = Intent(context, FeedActivity::class.java)
            intent.putExtra(FEED_ID_EXTRA, feed.id)
            return intent
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory

    lateinit var viewModel: FeedViewModel

    lateinit var adapter: PagedListAdapter<FeedItem, FeedViewHolder>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_feed)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        require(intent.hasExtra(FEED_ID_EXTRA)) {"FEED_ID_EXTRA is required to launch FeedActivity"}
        val feedId = intent.getStringExtra(FEED_ID_EXTRA)!!

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addItemDecoration(DividerItemDecoration(recyclerView.context, VERTICAL))

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(FeedViewModel::class.java)
        viewModel.setFeedId(feedId)
        viewModel.feed.observe(this, Observer { onFeed(it) })
        viewModel.feedItems.observe(this, Observer {
            adapter = FeedAdapter(it.feed, this::onFeedItemClicked)
            recyclerView.adapter = adapter
            adapter.submitList(it.pagedItems)
        })

        swipeContainer.setColorSchemeResources(R.color.md_blue_600, R.color.md_orange_A200)
        viewModel.refresh.observe(this, Observer {
            swipeContainer.isRefreshing = false
        })

        swipeContainer.setOnRefreshListener {
            viewModel.refresh()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun onFeed(feed: Feed) {
        title = feed.title
    }

    private fun onFeedItemClicked(feedItem: FeedItem) {
        val intent = FeedItemActivity.intent(this, feedItem)
        startActivity(intent)
    }
}