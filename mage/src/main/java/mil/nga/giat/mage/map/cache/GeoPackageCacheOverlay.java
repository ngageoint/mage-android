package mil.nga.giat.mage.map.cache;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import mil.nga.giat.mage.R;

/**
 * GeoPackage file cache overlay
 */
public class GeoPackageCacheOverlay extends CacheOverlay {

    /**
     * Mapping between table cache names and the table cache overlays
     */
    private Map<String, CacheOverlay> tables = new LinkedHashMap<String, CacheOverlay>();

    /**
     * Constructor
     *
     * @param name   GeoPackage name
     * @param tables tables
     */
    public GeoPackageCacheOverlay(String name, List<CacheOverlay> tables) {
        super(name, CacheOverlayType.GEOPACKAGE, true);

        for (CacheOverlay table : tables) {
            this.tables.put(table.getCacheName(), table);
        }
    }

    @Override
    public void removeFromMap() {
        for (CacheOverlay tableCacheOverlay : getChildren()) {
            tableCacheOverlay.removeFromMap();
        }
    }

    @Override
    public Integer getIconImageResourceId() {
        return R.drawable.ic_geopackage;
    }

    @Override
    public List<CacheOverlay> getChildren() {
        List<CacheOverlay> children = new ArrayList<>();
        children.addAll(tables.values());
        return children;
    }

}
