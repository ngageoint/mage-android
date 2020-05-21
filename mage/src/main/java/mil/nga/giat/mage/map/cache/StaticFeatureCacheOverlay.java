package mil.nga.giat.mage.map.cache;

public class StaticFeatureCacheOverlay extends CacheOverlay {

    private final Long myId;

    public StaticFeatureCacheOverlay(String name, Long id) {
        super(name, CacheOverlayType.STATIC_FEATURE, false);
        myId = id;
    }

    public Long getId() {return this.myId;}

    @Override
    public void removeFromMap() {

    }
}
