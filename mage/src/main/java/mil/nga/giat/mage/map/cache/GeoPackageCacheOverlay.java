package mil.nga.giat.mage.map.cache;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import mil.nga.giat.mage.R;

/**
 * GeoPackage file cache overlay
 *
 * @author osbornb
 */
public class GeoPackageCacheOverlay extends CacheOverlay {

    /**
     * Mapping between table cache names and the table cache overlays
     */
    private Map<String, CacheOverlay> tables = new LinkedHashMap<String, CacheOverlay>();

    private String filePath;

    /**
     * Constructor
     *
     * @param name   GeoPackage name
     * @param tables tables
     */
    public GeoPackageCacheOverlay(String name, String filePath, List<GeoPackageTableCacheOverlay> tables) {
        super(name, CacheOverlayType.GEOPACKAGE, true);

        this.filePath = filePath;

        for (GeoPackageTableCacheOverlay table : tables) {
            table.setParent(this);
            if(table.getType() == CacheOverlayType.GEOPACKAGE_FEATURE_TABLE){
                GeoPackageFeatureTableCacheOverlay featureTable = (GeoPackageFeatureTableCacheOverlay) table;
                for(GeoPackageTileTableCacheOverlay linkedTileTable: featureTable.getLinkedTileTables()){
                    linkedTileTable.setParent(this);
                }
            }
            this.tables.put(table.getCacheName(), table);
        }
    }

    public String getFilePath() {
        return filePath;
    }

    @Override
    public void removeFromMap() {
        for (CacheOverlay tableCacheOverlay : getChildren()) {
            tableCacheOverlay.removeFromMap();
        }
    }

    @Override
    public Integer getIconImageResourceId() {
        return R.drawable.ic_folder_open_black_24dp;
    }

    @Override
    public List<CacheOverlay> getChildren() {
        List<CacheOverlay> children = new ArrayList<>();
        children.addAll(tables.values());
        return children;
    }

}
