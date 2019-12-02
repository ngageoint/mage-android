package mil.nga.giat.mage.map;

import androidx.test.filters.SmallTest;

import junit.framework.TestCase;

import org.junit.Assert;
import org.junit.Test;

import java.net.URL;

import mil.nga.giat.mage.map.cache.URLCacheOverlay;
import mil.nga.giat.mage.map.cache.WMSCacheOverlay;
import mil.nga.giat.mage.sdk.datastore.layer.Layer;

@SmallTest
public class WMSTileProviderTest  extends TestCase {

    /**
     * Test that the tile provider can handle all defaults without blowing up
     *
     * @throws Exception
     */
    @Test
    public void testDefaults() throws Exception {
        Layer layer = new Layer();
        URLCacheOverlay wms =
                new WMSCacheOverlay("testDefaults", new URL("https://www.google.com"), layer);

        WMSTileProvider provider = new WMSTileProvider(256,256,wms);
        URL wmsURL = provider.getTileUrl(0,0,0);

        Assert.assertNotNull(wmsURL);
    }
}
