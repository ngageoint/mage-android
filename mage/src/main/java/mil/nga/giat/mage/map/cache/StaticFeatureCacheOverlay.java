package mil.nga.giat.mage.map.cache;

public class StaticFeatureCacheOverlay extends CacheOverlay {


    public StaticFeatureCacheOverlay(String name) {
        super(name, CacheOverlayType.STATIC_FEATURE, false);
    }

    @Override
    public void removeFromMap() {

    }
}
