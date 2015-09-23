package mil.nga.giat.mage.map.cache;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract cache overlay
 */
public abstract class CacheOverlay {

    /**
     * Name
     */
    private final String name;

    /**
     * Cache name
     */
    private final String cacheName;

    /**
     * Cache type
     */
    private final CacheOverlayType type;

    /**
     * True when enabled
     */
    private boolean enabled = false;

    /**
     * True if the cache type supports child caches
     */
    private final boolean supportsChildren;

    /**
     * Constructor
     *
     * @param name             name
     * @param type             cache type
     * @param supportsChildren true if cache overlay with children caches
     */
    protected CacheOverlay(String name, CacheOverlayType type, boolean supportsChildren) {
        this(name, name, type, supportsChildren);
    }

    /**
     * Constructor
     *
     * @param name             name
     * @param cacheName        cache name
     * @param type             cache type
     * @param supportsChildren true if cache overlay with children caches
     */
    protected CacheOverlay(String name, String cacheName, CacheOverlayType type, boolean supportsChildren) {
        this.name = name;
        this.cacheName = cacheName;
        this.type = type;
        this.supportsChildren = supportsChildren;
    }

    /**
     * Remove the cache overlay from the map
     */
    public abstract void removeFromMap();

    public String getName() {
        return name;
    }

    public String getCacheName() {
        return cacheName;
    }

    public CacheOverlayType getType() {
        return type;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Get the icon image resource id for the cache
     *
     * @return
     */
    public Integer getIconImageResourceId() {
        return null;
    }

    /**
     * Does the cache type support children
     *
     * @return
     */
    public boolean isSupportsChildren() {
        return supportsChildren;
    }

    /**
     * Get the children cache overlays
     *
     * @return
     */
    public List<CacheOverlay> getChildren() {
        return new ArrayList<>();
    }

    /**
     * Build the cache name of a child
     *
     * @param name      cache name
     * @param childName child cache name
     * @return
     */
    public static String buildChildCacheName(String name, String childName) {
        return name + "-" + childName;
    }

}