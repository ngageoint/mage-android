package mil.nga.giat.mage.map.preference

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.Checkable
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mil.nga.giat.mage.R
import mil.nga.giat.mage.data.repository.layer.LayerRepository
import mil.nga.giat.mage.database.model.event.Event
import mil.nga.giat.mage.map.cache.CacheOverlay
import mil.nga.giat.mage.map.cache.CacheProvider
import mil.nga.giat.mage.map.cache.CacheProvider.OnCacheOverlayListener
import mil.nga.giat.mage.map.cache.URLCacheOverlay
import mil.nga.giat.mage.database.model.layer.Layer
import mil.nga.giat.mage.data.datasource.layer.LayerLocalDataSource
import mil.nga.giat.mage.data.datasource.event.EventLocalDataSource
import java.util.Collections
import javax.inject.Inject

@AndroidEntryPoint
class OnlineLayersPreferenceActivity : AppCompatActivity() {
   @Inject lateinit var prefernces: SharedPreferences

   private var onlineLayersFragment: OnlineLayersListFragment? = null
   public override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setContentView(R.layout.activity_online_layers)
      onlineLayersFragment = supportFragmentManager.findFragmentById(R.id.online_layers_fragment) as OnlineLayersListFragment?
   }

   override fun onBackPressed() {
      val overlays = onlineLayersFragment?.selectedOverlays?.toSet() ?: emptySet()
      prefernces
         .edit()
         .putStringSet(resources.getString(R.string.onlineLayersKey), overlays)
         .apply()

      finish()
   }

   override fun onOptionsItemSelected(item: MenuItem): Boolean {
      return when (item.itemId) {
         android.R.id.home -> {
            onBackPressed()
            true
         }

         else -> super.onOptionsItemSelected(item)
      }
   }

   @AndroidEntryPoint
   class OnlineLayersListFragment : Fragment(), OnCacheOverlayListener {
      @Inject lateinit var preferences: SharedPreferences
      @Inject lateinit var cacheProvider: CacheProvider
      @Inject lateinit var layerRepository: LayerRepository
      @Inject lateinit var layerLocalDataSource: LayerLocalDataSource
      @Inject lateinit var eventLocalDataSource: EventLocalDataSource

      private var event: Event? = null
      private lateinit var adapter: OnlineLayersAdapter
      private lateinit var refreshButton: MenuItem
      private lateinit var contentView: View
      private lateinit var noContentView: View
      private lateinit var recyclerView: RecyclerView
      private lateinit var swipeContainer: SwipeRefreshLayout
      private var listState: Parcelable? = null

      override fun onCreate(savedInstanceState: Bundle?) {
         super.onCreate(savedInstanceState)
         event = eventLocalDataSource.currentEvent
      }

      override fun onCreateView(
         inflater: LayoutInflater,
         container: ViewGroup?,
         savedInstanceState: Bundle?
      ): View? {
         val view = inflater.inflate(R.layout.fragment_online_layers, container, false)
         contentView = view.findViewById(R.id.online_layers_content)
         noContentView = view.findViewById(R.id.online_layers_no_content)
         setHasOptionsMenu(true)
         swipeContainer = view.findViewById(R.id.swipeContainer)
         swipeContainer.setColorSchemeResources(R.color.md_blue_600, R.color.md_orange_A200)
         swipeContainer.setOnRefreshListener {
            softRefresh()
            hardRefresh()
         }
         recyclerView = view.findViewById(R.id.recycler_view)
         recyclerView.tag = "online"
         val mLayoutManager: RecyclerView.LayoutManager = LinearLayoutManager(activity)
         recyclerView.layoutManager = mLayoutManager
         recyclerView.itemAnimator = DefaultItemAnimator()
         adapter = OnlineLayersAdapter(requireContext().applicationContext, cacheProvider)
         return view
      }

      override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
         inflater.inflate(R.menu.online_layers_menu, menu)
         refreshButton = menu.findItem(R.id.online_layers_refresh)
         refreshButton.isEnabled = false
         cacheProvider.registerCacheOverlayListener(this, false)
         softRefresh()

         CoroutineScope(Dispatchers.IO).launch {
            cacheProvider.refreshTileOverlays()
         }
      }

      override fun onOptionsItemSelected(item: MenuItem): Boolean {
         if (item.itemId == R.id.online_layers_refresh) {
            softRefresh()
            hardRefresh()
            return true
         }
         return super.onOptionsItemSelected(item)
      }

      private fun softRefresh() {
         refreshButton.isEnabled = false
         swipeContainer.isRefreshing = true
         adapter.clear()
         adapter.notifyDataSetChanged()

         preferences
            .edit()
            .putStringSet(resources.getString(R.string.onlineLayersKey), selectedOverlays.toSet())
            .apply()
      }

      private fun hardRefresh() {
         CoroutineScope(Dispatchers.IO).launch {
            try {
               layerRepository.fetchImageryLayers()
               preferences
                  .edit()
                  .putStringSet(resources.getString(R.string.onlineLayersKey), selectedOverlays.toSet())
                  .apply()
               cacheProvider.refreshTileOverlays()
            } catch (e: Exception) {
               Log.w(LOG_NAME, "Failed fetching imagery", e)
            }
         }
      }

      override fun onCacheOverlay(cacheOverlays: List<CacheOverlay>) {
         activity?.runOnUiThread {
            val layers = layerLocalDataSource.readByEvent(event, "Imagery")

            adapter.clear()
            adapter.notifyDataSetChanged()
            val secureLayers = mutableListOf<Layer>()
            val insecureLayers = mutableListOf<Layer>()

            // Set what should be checked based on preferences.
            val overlays = preferences.getStringSet(resources.getString(R.string.onlineLayersKey), emptySet())
            for (layer in layers) {
               val enabled = overlays?.contains(layer.name) ?: false
               cacheProvider.getOverlay(layer.name)?.let { overlay ->
                  overlay.isEnabled = enabled
               }

               if (URLUtil.isHttpsUrl(layer.url)) {
                  secureLayers.add(layer)
               } else {
                  insecureLayers.add(layer)
               }
            }
            val compare = java.util.Comparator<Layer> { o1, o2 -> o1.name.compareTo(o2.name) }
            Collections.sort(secureLayers, compare)
            Collections.sort(insecureLayers, compare)
            adapter.addAllNonLayers(insecureLayers)
            adapter.addAllSecureLayers(secureLayers)
            adapter.notifyDataSetChanged()
            if (layers.isNotEmpty()) {
               noContentView.visibility = View.GONE
               contentView.visibility = View.VISIBLE
            } else {
               contentView.visibility = View.GONE
               noContentView.visibility = View.VISIBLE
            }
            refreshButton.isEnabled = true
            swipeContainer.isRefreshing = false
         }
      }

      val selectedOverlays: ArrayList<String>
         get() {
            val overlays = ArrayList<String>()
            cacheProvider.cacheOverlays.let { overlay ->
               if (overlay is URLCacheOverlay) {
                  if (overlay.isEnabled) {
                     overlays.add(overlay.name)
                  }
               }
            }

            return overlays
         }

      override fun onDestroy() {
         super.onDestroy()
         cacheProvider.unregisterCacheOverlayListener(this)
      }

      override fun onResume() {
         super.onResume()
         recyclerView.adapter = adapter
         if (listState != null) {
            recyclerView.layoutManager!!.onRestoreInstanceState(listState)
         }
      }

      override fun onPause() {
         super.onPause()
         listState = recyclerView.layoutManager!!.onSaveInstanceState()
         recyclerView.adapter = null
      }
   }

   /**
    * ALL public methods MUST be made on the UI thread to ensure concurrency.**
    */
   class OnlineLayersAdapter internal constructor(
      private val context: Context,
      private val cacheProvider: CacheProvider
   ): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
      private val secureLayers = mutableListOf<Layer>()
      private val nonSecureLayers = mutableListOf<Layer>()

      fun clear() {
         secureLayers.clear()
         nonSecureLayers.clear()
      }

      override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
         return if (viewType == ITEM_TYPE_LAYER) {
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.online_layers_list_item, parent, false)
            LayerViewHolder(itemView)
         } else {
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.event_list_section_header, parent, false)
            SectionViewHolder(itemView)
         }
      }

      override fun onBindViewHolder(holder: RecyclerView.ViewHolder, i: Int) {
         if (holder is LayerViewHolder) {
            bindLayerViewHolder(holder, i)
         } else if (holder is SectionViewHolder) {
            bindSectionViewHolder(holder, i)
         }
      }

      private fun bindLayerViewHolder(holder: LayerViewHolder, i: Int) {
         val view = holder.itemView
         val layer = if (i <= secureLayers.size) {
            secureLayers[i - 1]
         } else {
            nonSecureLayers[i - secureLayers.size - 2]
         }
         val title = view.findViewById<TextView>(R.id.online_layers_title)
         title.text = layer.name
         val summary = view.findViewById<TextView>(R.id.online_layers_summary)
         summary.text = layer.url
         val toggle = view.findViewById<View>(R.id.online_layers_toggle)
         if (URLUtil.isHttpUrl(layer.url)) {
            view.setOnClickListener {
               AlertDialog.Builder(context)
                  .setTitle("Non HTTPS Layer")
                  .setMessage("We cannot load this layer on mobile because it cannot be accessed securely.")
                  .setPositiveButton("OK", null).show()
            }
            toggle.setOnClickListener(null)
            toggle.isEnabled = false
            (toggle as Checkable).isChecked = false
         } else {
            view.setOnClickListener { toggle.performClick() }
            toggle.setOnClickListener { v ->
               val isChecked = (v as Checkable).isChecked
               cacheProvider.getOverlay(layer.name)?.let { overlay ->
                  overlay.isEnabled = isChecked
               }
            }
            toggle.isEnabled = true
            cacheProvider.getOverlay(layer.name)?.let { overlay ->
               (toggle as Checkable).isChecked = overlay.isEnabled
            }
         }
      }

      private fun bindSectionViewHolder(holder: SectionViewHolder, position: Int) {
         holder.sectionName.text = if (position == 0) "Secure Layers" else "Nonsecure Layers"
      }

      fun addAllSecureLayers(layers: List<Layer>?) {
         secureLayers.addAll(layers!!)
      }

      fun addAllNonLayers(layers: List<Layer>?) {
         nonSecureLayers.addAll(layers!!)
      }

      override fun getItemCount(): Int {
         return secureLayers.size + nonSecureLayers.size + 2
      }

      override fun getItemViewType(position: Int): Int {
         return if (position == 0 || position == secureLayers.size + 1) {
            ITEM_TYPE_HEADER
         } else {
            ITEM_TYPE_LAYER
         }
      }

      private inner class LayerViewHolder(view: View?) : RecyclerView.ViewHolder(view!!)
      private inner class SectionViewHolder(view: View) :
         RecyclerView.ViewHolder(view) {
         val sectionName: TextView

         init {
            sectionName = view.findViewById(R.id.section_name)
         }
      }

      companion object {
         private const val ITEM_TYPE_HEADER = 1
         private const val ITEM_TYPE_LAYER = 2
      }
   }

   companion object {
      private val LOG_NAME = OnlineLayersPreferenceActivity::class.java.name
   }
}