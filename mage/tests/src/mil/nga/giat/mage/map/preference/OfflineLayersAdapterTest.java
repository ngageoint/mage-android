package mil.nga.giat.mage.map.preference;

import android.content.Context;


import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import mil.nga.giat.mage.map.cache.CacheOverlay;
import mil.nga.giat.mage.map.cache.CacheOverlayType;
import mil.nga.giat.mage.sdk.datastore.layer.Layer;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

/**
 * @see OnlineLayersPreferenceActivityTest
 * @since  11/22/2019
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class OfflineLayersAdapterTest {

    @Test
    public void testInit() {
        Context context = getApplicationContext();
        Assert.assertNotNull(context);

        OfflineLayersAdapter adapter = new OfflineLayersAdapter(context, null);

        Assert.assertNotNull(adapter.getDownloadableLayers());
        Assert.assertNotNull(adapter.getSideloadedOverlays());
        Assert.assertNotNull(adapter.getOverlays());

        Assert.assertEquals(0, adapter.getGroupCount());
    }

    @Test
    public void testGroups() {
        Context context = getApplicationContext();
        Assert.assertNotNull(context);

        OfflineLayersAdapter adapter = new OfflineLayersAdapter(context, null);

        CacheOverlay first = new CacheOverlay("first", CacheOverlayType.STATIC_FEATURE, false) {
            @Override
            public void removeFromMap() {

            }
        };
        CacheOverlay child = new CacheOverlay("child", CacheOverlayType.XYZ_DIRECTORY, false) {
            @Override
            public void removeFromMap() {

            }
        };
        CacheOverlay second = new CacheOverlay("second", CacheOverlayType.XYZ_DIRECTORY, true) {

            private List<CacheOverlay> children = new ArrayList<>();

            @Override
            public void removeFromMap() {

            }

            public List<CacheOverlay> getChildren() {
                return  children;
            }
        };
        second.getChildren().add(child);
        Layer third = new Layer();
        third.setType("test");

        adapter.getOverlays().add(first);
        adapter.getSideloadedOverlays().add(second);
        adapter.getDownloadableLayers().add(third);

        Assert.assertEquals(3, adapter.getGroupCount());
        Assert.assertEquals(first, adapter.getGroup(0));
        Assert.assertEquals(second, adapter.getGroup(1));
        Assert.assertEquals(third, adapter.getGroup(2));

        Assert.assertEquals(1, adapter.getChildrenCount(1));
        Assert.assertEquals(child, adapter.getChild(1, 0));

    }
}
