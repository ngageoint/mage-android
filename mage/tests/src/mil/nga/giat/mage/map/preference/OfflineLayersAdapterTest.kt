package mil.nga.giat.mage.map.preference

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.mockk
import mil.nga.giat.mage.database.model.layer.Layer
import mil.nga.giat.mage.map.cache.CacheOverlay
import mil.nga.giat.mage.map.cache.CacheOverlayType
import mil.nga.giat.mage.map.cache.CacheProvider
import mil.nga.giat.mage.map.cache.StaticFeatureCacheOverlay
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class OfflineLayersAdapterTest {
   private lateinit var cacheProvider: CacheProvider

   @Before
   fun setUp() {
      cacheProvider = CacheProvider(
         application = mockk(),
         layerLocalDataSource = mockk(),
         eventLocalDataSource = mockk(),
         preferences = mockk()
      )
   }

   @Test
   fun testInit() {
      val context = ApplicationProvider.getApplicationContext<Context>()

      val adapter = OfflineLayersAdapter(
         context= context,
         cacheProvider = cacheProvider,
         downloadManager = mockk(),
         layerRepository = mockk(),
         layerLocalDataSource = mockk(),
         event = mockk()
      )

      Assert.assertNotNull(adapter.downloadableLayers)
      Assert.assertNotNull(adapter.sideloadedOverlays)
      Assert.assertNotNull(adapter.overlays)
      Assert.assertEquals(0, adapter.groupCount.toLong())
   }

   @Test
   fun testGroups() {
      val context = ApplicationProvider.getApplicationContext<Context>()

      val adapter = OfflineLayersAdapter(
         context= context,
         cacheProvider = cacheProvider,
         downloadManager = mockk(),
         layerRepository = mockk(),
         layerLocalDataSource = mockk(),
         event = mockk()
      )

      val first = object : CacheOverlay("first", CacheOverlayType.STATIC_FEATURE, false) {
         override fun removeFromMap() {}
      }

      val child = object : CacheOverlay("child", CacheOverlayType.XYZ_DIRECTORY, false) {
         override fun removeFromMap() {}
      }

      val second = object : CacheOverlay("second", CacheOverlayType.XYZ_DIRECTORY, true) {
         private val children: List<CacheOverlay> = listOf(child)
         override fun removeFromMap() {}
         override fun getChildren() = children
      }

      val third = Layer()
      third.type = "test"
      adapter.overlays.add(first)
      adapter.sideloadedOverlays.add(second)
      adapter.downloadableLayers.add(third)
      Assert.assertEquals(3, adapter.groupCount.toLong())
      Assert.assertEquals(first, adapter.getGroup(0))
      Assert.assertEquals(second, adapter.getGroup(1))
      Assert.assertEquals(third, adapter.getGroup(2))
      Assert.assertEquals(1, adapter.getChildrenCount(1).toLong())
      Assert.assertEquals(child, adapter.getChild(1, 0))
   }

   @Test
   fun testAddOverlay() {
      val context = ApplicationProvider.getApplicationContext<Context>()

      val adapter = OfflineLayersAdapter(
         context= context,
         cacheProvider = cacheProvider,
         downloadManager = mockk(),
         layerRepository = mockk(),
         layerLocalDataSource = mockk(),
         event = mockk()
      )

      adapter.addOverlay(null, Layer())
      Assert.assertEquals(0, adapter.overlays.size.toLong())
      val existing = Layer()
      existing.isLoaded = true
      adapter.downloadableLayers.add(existing)
      val overlay: CacheOverlay = StaticFeatureCacheOverlay("test", 12345L)
      adapter.addOverlay(overlay, existing)
      Assert.assertEquals(0, adapter.downloadableLayers.size.toLong())
      Assert.assertEquals(1, adapter.overlays.size.toLong())
      Assert.assertEquals(overlay, adapter.overlays[0])
   }
}
