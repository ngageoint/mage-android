package mil.nga.giat.mage.feed.item

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.TypedValue
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
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
import dagger.android.support.DaggerAppCompatActivity
import kotlinx.android.synthetic.main.activity_feed_item.*
import mil.nga.geopackage.map.geom.GoogleMapShapeConverter
import mil.nga.giat.mage.R
import mil.nga.giat.mage.data.feed.FeedItem
import mil.nga.giat.mage.data.feed.ItemWithFeed
import mil.nga.giat.mage.glide.target.MarkerTarget
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
    private lateinit var itemWithFeed: ItemWithFeed
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var dateFormat: DateFormat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_feed_item)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        require(intent.hasExtra(FEED_ID_EXTRA)) {"FEED_ID_EXTRA is required to launch FeedActivity"}
        require(intent.hasExtra(FEED_ITEM_ID_EXTRA)) {"FEED_ITEM_ID_EXTRA is required to launch FeedActivity"}
        val feedId = intent.getStringExtra(FEED_ID_EXTRA)!!
        val feedItemId = intent.getStringExtra(FEED_ITEM_ID_EXTRA)!!

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
        this.itemWithFeed = itemWithFeed
        val feed = itemWithFeed.feed

        val propertyFactory = FeedItemPropertyFactory(applicationContext)
        val properties = if (itemWithFeed.item.properties?.isJsonNull == false) {
            itemWithFeed.item.properties.asJsonObject.entrySet().map { property ->
                propertyFactory.createFeedItemProperty(feed, property.toPair())
            }
        } else emptyList()
        header.visibility = if (hasHeaderContent()) View.VISIBLE else View.GONE

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
        if (feed.itemsHaveSpatialDimension && itemWithFeed.item.geometry != null) {
            mapFragment.getMapAsync(this)
            location.setLatLng(LatLng(itemWithFeed.item.geometry.centroid.y, itemWithFeed.item.geometry.centroid.x))

            icon.visibility = View.GONE
        } else {
            location_layout.visibility = View.GONE
            mapFragment.view?.visibility = View.GONE

            icon.visibility = View.VISIBLE
            Glide.with(this)
               .load(feed.mapStyle?.iconUrl)
               .placeholder(R.drawable.default_marker)
               .fitCenter()
               .into(icon)
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

        val geometry = itemWithFeed.item.geometry!!
        val location = ObservationLocation(geometry)
        map.moveCamera(location.getCameraUpdate(mapFragment.view))

        when(geometry.geometryType) {
            GeometryType.POINT -> {
                val point = GeometryUtils.getCentroid(geometry)
                val marker = map.addMarker(MarkerOptions()
                   .position(LatLng(point.y, point.x))
                   .visible(false))

                marker.tag = itemWithFeed.item

                Glide.with(this)
                   .asBitmap()
                   .load(itemWithFeed.feed.mapStyle?.iconUrl)
                   .error(R.drawable.default_marker)
                   .into(MarkerTarget(applicationContext, marker, 24, 24))
            }
            else -> {
                val shapeConverter = GoogleMapShapeConverter()
                val shape = shapeConverter.toShape(geometry)
                GoogleMapShapeConverter.addShapeToMap(map, shape)
            }
        }
    }

    private fun hasHeaderContent(): Boolean {
        val item = itemWithFeed.item

        return if (item.properties?.isJsonObject == true) {
            val feed = itemWithFeed.feed
            val properties = item.properties.asJsonObject

            (feed.itemTemporalProperty != null && properties?.get(feed.itemTemporalProperty) != null) ||
               (feed.itemPrimaryProperty != null && properties?.get(feed.itemPrimaryProperty) != null) ||
               (feed.itemSecondaryProperty != null && properties?.get(feed.itemSecondaryProperty) != null)
        } else false
    }

    private fun onLocationClick() {
        val location: String = location.text.toString()
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        val clip = ClipData.newPlainText("Feed Item Location", location)

        if (clipboard != null && clip != null) {
            clipboard.setPrimaryClip(clip)
            Snackbar.make(findViewById(R.id.coordinator_layout), R.string.location_text_copy_message, Snackbar.LENGTH_SHORT).show()
        }
    }
}