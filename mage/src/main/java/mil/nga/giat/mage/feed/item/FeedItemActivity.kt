package mil.nga.giat.mage.feed.item

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.gson.JsonElement
import dagger.android.support.DaggerAppCompatActivity
import kotlinx.android.synthetic.main.activity_feed.*
import kotlinx.android.synthetic.main.activity_feed_item.*
import kotlinx.android.synthetic.main.activity_feed_item.recyclerView
import kotlinx.android.synthetic.main.feed_list_item.view.*
import mil.nga.geopackage.map.geom.GoogleMapShapeConverter
import mil.nga.giat.mage.R
import mil.nga.giat.mage.data.feed.FeedItem
import mil.nga.giat.mage.data.feed.ItemWithFeed
import mil.nga.giat.mage.observation.ObservationLocation
import mil.nga.giat.mage.utils.DateFormatFactory
import mil.nga.sf.GeometryType
import mil.nga.sf.util.GeometryUtils
import java.text.DateFormat
import java.util.*
import javax.inject.Inject

class FeedItemActivity: DaggerAppCompatActivity(), OnMapReadyCallback {
    companion object {
        private const val FEED_ID_EXTRA = "FEED_ID_EXTRA"
        private const val FEED_ITEM_ID_EXTRA = "FEED_ITEM_ID_EXTRA"

        fun intent(context: Context, feedItem: FeedItem): Intent {
            val intent = Intent(context, FeedItemActivity::class.java)
            intent.putExtra(FEED_ID_EXTRA, feedItem.feedId)
            intent.putExtra(FEED_ITEM_ID_EXTRA, feedItem.id)
            return intent
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: FeedItemViewModel

    private val adapter = FeedItemAdapter()
    private lateinit var feedItem: FeedItem
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var dateFormat: DateFormat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_feed_item)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        require(intent.hasExtra(FEED_ID_EXTRA)) {"FEED_ID_EXTRA is required to launch FeedActivity"}
        require(intent.hasExtra(FEED_ITEM_ID_EXTRA)) {"FEED_ITEM_ID_EXTRA is required to launch FeedActivity"}
        val feedId = intent.extras.getString(FEED_ID_EXTRA)!!
        val feedItemId = intent.extras.getString(FEED_ITEM_ID_EXTRA)!!

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        recyclerView.addItemDecoration(DividerItemDecoration(recyclerView.context, DividerItemDecoration.VERTICAL))

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(FeedItemViewModel::class.java)
        viewModel.getFeedItem(feedId, feedItemId).observe(this, Observer { onFeedItem(it) })
        dateFormat = DateFormatFactory.format("yyyy-MM-dd HH:mm zz", Locale.getDefault(), applicationContext)

        location_layout.setOnClickListener {
            onLocationClick()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun onFeedItem(itemWithFeed: ItemWithFeed) {
        val feed = itemWithFeed.feed
        this.feedItem = itemWithFeed.item

        val propertyFactory = FeedItemPropertyFactory(applicationContext)
        val properties = if (itemWithFeed.item.properties?.isJsonNull == false) {
            itemWithFeed.item.properties.asJsonObject.entrySet().map { property ->
                propertyFactory.createFeedItemProperty(feed, property.toPair())
            }
        } else emptyList()
        header.visibility = if (properties.isEmpty()) View.GONE else View.VISIBLE

        Glide.with(this)
            .load(feed.style?.iconUrl)
            .placeholder(R.drawable.default_marker_24)
            .fitCenter()
            .into(icon)

        if (itemWithFeed.item.properties?.isJsonObject == true) {
            if (feed.itemTemporalProperty != null) {
                overline.visibility = View.VISIBLE
                itemWithFeed.item.properties.asJsonObject.get(feed.itemTemporalProperty)?.asLong?.let {
                    overline.text = dateFormat.format(it)
                }
            }

            if (feed.itemPrimaryProperty != null) {
                primary.visibility = View.VISIBLE
                primary.text = itemWithFeed.item.properties.asJsonObject.get(feed.itemPrimaryProperty)?.asString
            }

            if (feed.itemSecondaryProperty != null) {
                secondary.visibility = View.VISIBLE
                secondary.text = itemWithFeed.item.properties.asJsonObject.get(feed.itemSecondaryProperty)?.asString
            }
        }

        mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        if (itemWithFeed.feed.itemsHaveSpatialDimension && itemWithFeed.item.geometry != null) {
            mapFragment.getMapAsync(this)
            location.setLatLng(LatLng(itemWithFeed.item.geometry.centroid.y, itemWithFeed.item.geometry.centroid.x))
        } else {
            location_layout.visibility = View.GONE
            mapFragment.view?.visibility = View.GONE
        }

        adapter.submitList(properties)
    }

    override fun onMapReady(map: GoogleMap) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        map.mapType = preferences.getInt(getString(R.string.baseLayerKey), resources.getInteger(R.integer.baseLayerDefaultValue))

        val dayNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (dayNightMode == Configuration.UI_MODE_NIGHT_NO) {
            map.setMapStyle(null)
        } else {
            map.setMapStyle(MapStyleOptions.loadRawResourceStyle(applicationContext, R.raw.map_theme_night))
        }

        val geometry = feedItem.geometry!!
        val location = ObservationLocation(geometry)
        map.moveCamera(location.getCameraUpdate(mapFragment.view))

        when(geometry.geometryType) {
            GeometryType.POINT -> {
                val point = GeometryUtils.getCentroid(geometry)
                val latLng = LatLng(point.y, point.x)
                map.addMarker(MarkerOptions().position(latLng))
            }
            else -> {
                val shapeConverter = GoogleMapShapeConverter()
                val shape = shapeConverter.toShape(geometry)
                GoogleMapShapeConverter.addShapeToMap(map, shape)
            }
        }
    }

    private fun onLocationClick() {
        val location: String = location.text.toString()
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        // TODO Change text from "Feed Item Location"
        val clip = ClipData.newPlainText("Feed Item Location", location)
        if (clipboard == null || clip == null) return
        clipboard.primaryClip = clip
        Snackbar.make(findViewById(R.id.coordinator_layout), R.string.location_text_copy_message, Snackbar.LENGTH_SHORT).show()
    }
}