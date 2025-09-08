package mil.nga.giat.mage.map.cache

import io.mockk.mockk
import junit.framework.TestCase
import java.util.concurrent.Semaphore

class CacheProviderTest : TestCase() {
   private lateinit var cacheProvider: CacheProvider
   private lateinit var testOverlay: CacheOverlay

   @Throws(Exception::class)
   public override fun setUp() {
      super.setUp()

      cacheProvider = CacheProvider(
         application = mockk(),
         layerLocalDataSource = mockk(),
         eventLocalDataSource = mockk(),
         preferences = mockk()
      )

      testOverlay = object : CacheOverlay("test", CacheOverlayType.XYZ_DIRECTORY, false) {
         override fun removeFromMap() {}
      }

      ourLock.drainPermits()
   }

   public override fun tearDown() {}

   fun testGetOverlay() {
      val overlay = cacheProvider.getOverlay("doesNotExist")
      assertNull(overlay)
   }

   @Throws(Exception::class)
   fun testAddCacheOverlay() {
      cacheProvider.addCacheOverlay(testOverlay)
      assertNotNull(cacheProvider.getOverlay(testOverlay.cacheName))

      val foundOverlays: MutableList<CacheOverlay> = ArrayList()
      val listener = object : CacheProvider.OnCacheOverlayListener {
         override fun onCacheOverlay(cacheOverlays: List<CacheOverlay>) {
            foundOverlays.addAll(cacheOverlays)
            ourLock.release()
         }
      }
      cacheProvider.registerCacheOverlayListener(listener)
      ourLock.acquire()
      assertTrue(foundOverlays.size == 1)
      assertTrue(foundOverlays[0] === testOverlay)
      cacheProvider.unregisterCacheOverlayListener(listener)
   }

   @Throws(Exception::class)
   fun testRemoveCacheOverlay() {
      cacheProvider.addCacheOverlay(testOverlay)
      assertNotNull(cacheProvider.getOverlay(testOverlay.cacheName))

      val foundOverlays: MutableList<CacheOverlay> = ArrayList()
      val listener = object : CacheProvider.OnCacheOverlayListener {
         override fun onCacheOverlay(cacheOverlays: List<CacheOverlay>) {
            foundOverlays.addAll(cacheOverlays)
            ourLock.release()
         }
      }
      cacheProvider.registerCacheOverlayListener(listener)
      ourLock.acquire()
      assertTrue(foundOverlays.size == 1)
      assertTrue(foundOverlays[0] === testOverlay)
      foundOverlays.clear()
      cacheProvider.unregisterCacheOverlayListener(listener)
      cacheProvider.removeCacheOverlay(testOverlay.cacheName)
      cacheProvider.registerCacheOverlayListener(listener)
      ourLock.acquire()
      assertTrue(foundOverlays.isEmpty())
      cacheProvider.unregisterCacheOverlayListener(listener)
   }

   companion object {
      /**
       * Used to control synchronization with the callbacks
       */
      private val ourLock = Semaphore(1)
   }
}