package mil.nga.giat.mage.map.cache;

import android.content.Context;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;

import org.apache.commons.lang3.builder.CompareToBuilder;

import java.util.ArrayList;
import java.util.List;

import mil.nga.geopackage.BoundingBox;
import mil.nga.giat.mage.map.GeoPackageFeatureMapState;

/**
 * Abstract cache overlay
 *
 * @author osbornb
 */
public abstract class CacheOverlay implements Comparable<CacheOverlay> {

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
     * True when the cache was newly added, such as a file opened with MAGE
     */
    private boolean added = false;

    /**
     * True if the cache type supports child caches
     */
    private final boolean supportsChildren;

    private boolean isSideloaded = false;

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

    public boolean isAdded() {
        return added;
    }

    public void setAdded(boolean added) {
        this.added = added;
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
     * Return true if a child cache overlay, false if a top level with or without children
     *
     * @return true if a child
     */
    public boolean isChild(){
        return false;
    }

    /**
     * Get the child's parent cache overlay
     *
     * @return parent cache overlay
     */
    public CacheOverlay getParent(){
        return null;
    }

    /**
     * Get information about the cache to display
     *
     * @return
     */
    public String getInfo() {
        return null;
    }

    /**
     * On map click
     *
     * @param latLng  map click location
     * @param mapView map view
     * @param map     Google map
     * @return map click message
     */
    public String onMapClick(LatLng latLng, MapView mapView, GoogleMap map) {
        return null;
    }

    public List<GeoPackageFeatureMapState> getFeaturesNearClick(LatLng latLng, MapView mapView, GoogleMap map, Context context) {
        return new ArrayList<>();
    }

    public List<GeoPackageFeatureMapState> getFeatures(
            LatLng latLng,
            BoundingBox boundingBox,
            Float zoom,
            Context context
    ) {
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

    public boolean isSideloaded() {
        return isSideloaded;
    }

    public void setSideloaded(boolean sideloaded) {
        isSideloaded = sideloaded;
    }

    @Override
    public int compareTo(CacheOverlay o) {
        return new CompareToBuilder().append(this.getCacheName(), o.getCacheName()).toComparison();
    }
}