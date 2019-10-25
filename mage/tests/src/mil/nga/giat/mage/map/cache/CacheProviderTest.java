package mil.nga.giat.mage.map.cache;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class CacheProviderTest extends TestCase {

    /**
     * Used to control synchronization with the callbacks
     */
    private static final Semaphore ourLock = new Semaphore(1);

    /**
     * Overlay used within the test
     */
    private CacheOverlay myTestOverlay = null;


    @Override
    public void setUp() throws Exception {
        super.setUp();

        myTestOverlay = new CacheOverlay("test", CacheOverlayType.XYZ_DIRECTORY, false) {
            @Override
            public void removeFromMap() {

            }
        };
        ourLock.drainPermits();
    }

    @Override
    public void tearDown() throws Exception {
    }

    public void testGetCacheOverlays() {
        List<CacheOverlay> overlayList = CacheProvider.getInstance(null).getCacheOverlays();
        assertNotNull(overlayList);
        try {
            overlayList.add(myTestOverlay);
            fail("List should not be modifiable");
        } catch (Exception e) {

        }
    }

    public void testGetOverlay() {
        CacheOverlay overlay = CacheProvider.getInstance(null).getOverlay("doesNotExist");
        assertNull(overlay);
    }

    public void testAddCacheOverlay() throws Exception {
        CacheProvider.getInstance(null).addCacheOverlay(myTestOverlay);
        assertNotNull(CacheProvider.getInstance(null).getOverlay(myTestOverlay.getCacheName()));

        final List<CacheOverlay> foundOverlays = new ArrayList<>();

        CacheProvider.OnCacheOverlayListener listener = new CacheProvider.OnCacheOverlayListener() {
            @Override
            public void onCacheOverlay(List<CacheOverlay> cacheOverlays) {
                foundOverlays.addAll(cacheOverlays);
                ourLock.release();
            }
        };

        CacheProvider.getInstance(null).registerCacheOverlayListener(listener);
        ourLock.acquire();
        assertTrue(foundOverlays.size() == 1);
        assertTrue(foundOverlays.get(0) == myTestOverlay);

        CacheProvider.getInstance(null).unregisterCacheOverlayListener(listener);
    }

    public void testRemoveCacheOverlay() throws Exception {
        CacheProvider.getInstance(null).addCacheOverlay(myTestOverlay);
        assertNotNull(CacheProvider.getInstance(null).getOverlay(myTestOverlay.getCacheName()));

        final List<CacheOverlay> foundOverlays = new ArrayList<>();

        CacheProvider.OnCacheOverlayListener listener = new CacheProvider.OnCacheOverlayListener() {
            @Override
            public void onCacheOverlay(List<CacheOverlay> cacheOverlays) {
                foundOverlays.addAll(cacheOverlays);
                ourLock.release();
            }
        };

        CacheProvider.getInstance(null).registerCacheOverlayListener(listener);
        ourLock.acquire();
        assertTrue(foundOverlays.size() == 1);
        assertTrue(foundOverlays.get(0) == myTestOverlay);

        foundOverlays.clear();

        CacheProvider.getInstance(null).unregisterCacheOverlayListener(listener);
        CacheProvider.getInstance(null).removeCacheOverlay(myTestOverlay.getCacheName());
        CacheProvider.getInstance(null).registerCacheOverlayListener(listener);
        ourLock.acquire();
        assertTrue(foundOverlays.isEmpty());

        CacheProvider.getInstance(null).unregisterCacheOverlayListener(listener);
    }
}