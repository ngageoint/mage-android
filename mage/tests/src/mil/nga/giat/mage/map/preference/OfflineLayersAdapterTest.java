package mil.nga.giat.mage.map.preference;

import android.content.Context;


import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

/**
 * @see OnlineLayersPreferenceActivityTest
 * @since  11/22/2019
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class OfflineLayersAdapterTest {

    @Test
    public void testAdapterInit() {
        Context context = getApplicationContext();
        Assert.assertNotNull(context);

        OfflineLayersAdapter adapter = new OfflineLayersAdapter(context, null);

        Assert.assertNotNull(adapter.getDownloadableLayers());
        Assert.assertNotNull(adapter.getSideloadedOverlays());
        Assert.assertNotNull(adapter.getOverlays());

        Assert.assertEquals(0, adapter.getGroupCount());
    }

    @Test
    public void testAdapter() {
        Context context = getApplicationContext();
        Assert.assertNotNull(context);

        OfflineLayersAdapter adapter = new OfflineLayersAdapter(context, null);
    }
}
