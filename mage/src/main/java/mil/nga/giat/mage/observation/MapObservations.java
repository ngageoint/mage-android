package mil.nga.giat.mage.observation;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;
import com.google.maps.android.PolyUtil;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import mil.nga.geopackage.map.geom.GoogleMapShape;
import mil.nga.geopackage.map.geom.GoogleMapShapeType;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;

/**
 * Collection of map observations including markers and shapes
 *
 * @author osbornb
 */
public class MapObservations {

    /**
     * Map between observation ids and map observations
     */
    private Map<Long, MapObservation> observationIds = new HashMap<>();

    /**
     * Map between marker ids and map marker observations
     */
    private Map<String, MapMarkerObservation> markerIds = new HashMap<>();

    /**
     * Temporary shape marker for selected marker
     */
    private Marker shapeMarker = null;

    /**
     * Temporary shape observation from selected shape marker
     */
    private MapShapeObservation shapeObservation = null;

    /**
     * Constructor
     */
    public MapObservations() {

    }

    /**
     * Add a map observation
     *
     * @param mapObservation map observation
     */
    public void add(MapObservation mapObservation) {
        long id = mapObservation.getObservation().getId();
        observationIds.put(id, mapObservation);
        if (isMarker(mapObservation)) {
            MapMarkerObservation mapMarkerObservation = (MapMarkerObservation) mapObservation;
            markerIds.put(mapMarkerObservation.getMarker().getId(), mapMarkerObservation);
        }
    }

    /**
     * Get a map observation by id
     *
     * @param observationId observation id
     * @return map observation
     */
    public MapObservation get(long observationId) {
        return observationIds.get(observationId);
    }

    /**
     * Get an observation by marker id, either a marker observation or a selected shape marker
     *
     * @param markerId marker id
     * @return observation
     */
    public Observation getMarkerObservation(String markerId) {
        Observation observation = null;
        MapMarkerObservation mapMarkerObservation = markerIds.get(markerId);
        if (mapMarkerObservation != null) {
            observation = mapMarkerObservation.getObservation();
        } else if (shapeMarker != null && markerId.equals(shapeMarker.getId())) {
            observation = shapeObservation.getObservation();
        }
        return observation;
    }

    /**
     * Check if the map observation is a marker
     *
     * @param mapObservation map observation
     * @return true if a marker observation
     */
    public static boolean isMarker(MapObservation mapObservation) {
        return mapObservation instanceof MapMarkerObservation;
    }

    /**
     * Check if the map observation is a shape
     *
     * @param mapObservation map observation
     * @return true if a shape observation
     */
    public static boolean isShape(MapObservation mapObservation) {
        return mapObservation instanceof MapShapeObservation;
    }

    /**
     * Remove the map observation
     *
     * @param observationId observation id
     * @return removed map observation
     */
    public MapObservation remove(long observationId) {
        MapObservation mapObservation = observationIds.remove(observationId);
        if (mapObservation != null) {
            if (isMarker(mapObservation)) {
                MapMarkerObservation mapMarkerObservation = (MapMarkerObservation) mapObservation;
                markerIds.remove(mapMarkerObservation.getMarker().getId());
            }
            mapObservation.remove();
        }

        return mapObservation;
    }

    /**
     * Set the visibility on all map observations
     *
     * @param visible visible flag
     */
    public void setVisible(boolean visible) {
        for (MapObservation mapObservation : observationIds.values()) {
            mapObservation.setVisible(visible);
        }
    }

    /**
     * Set the temporary marker for a selected shape
     *
     * @param shapeMarker      shape marker
     * @param shapeObservation shape observation
     */
    public void setShapeMarker(Marker shapeMarker, MapShapeObservation shapeObservation) {
        clearShapeMarker();
        this.shapeMarker = shapeMarker;
        this.shapeObservation = shapeObservation;
    }

    /**
     * Clear the shape marker from the map
     */
    public void clearShapeMarker() {
        if (shapeMarker != null) {
            shapeMarker.remove();
            shapeMarker = null;
        }
        shapeObservation = null;
    }

    /**
     * Clear all map observations from the map and collections
     */
    public void clear() {
        clearShapeMarker();
        for (MapObservation shape : observationIds.values()) {
            shape.remove();
        }
        observationIds.clear();
        markerIds.clear();
    }

    /**
     * Get the clicked shape from the click location
     *
     * @param map    map
     * @param latLng click location
     * @return map shape observation
     */
    public MapShapeObservation getClickedShape(GoogleMap map, LatLng latLng) {

        MapShapeObservation mapShapeObservation = null;

        // TODO
        // how many meters away form the click can the geometry be?
        Double circumferenceOfEarthInMeters = 2 * Math.PI * 6371000;
        // Double tileWidthAtZoomLevelAtEquatorInDegrees = 360.0/Math.pow(2.0, map.getCameraPosition().zoom);
        Double pixelSizeInMetersAtLatitude = (circumferenceOfEarthInMeters * Math.cos(map.getCameraPosition().target.latitude * (Math.PI / 180.0))) / Math.pow(2.0, map.getCameraPosition().zoom + 8.0);
        Double tolerance = pixelSizeInMetersAtLatitude * Math.sqrt(2.0) * 10.0;

        for (MapShapeObservation observationShape : getShapes()) {

            GoogleMapShape mapShape = observationShape.getShape();
            GoogleMapShapeType shapeType = mapShape.getShapeType();
            switch (shapeType) {
                case POLYLINE:
                    Polyline polyline = (Polyline) mapShape.getShape();
                    if (PolyUtil.isLocationOnPath(latLng, polyline.getPoints(), true, tolerance)) {
                        mapShapeObservation = observationShape;
                    }
                    break;
                case POLYGON:
                    Polygon polygon = (Polygon) mapShape.getShape();
                    if (PolyUtil.containsLocation(latLng, polygon.getPoints(), true)) {
                        mapShapeObservation = observationShape;
                    }
                    break;
            }
        }

        // Find closest to center or do lines first?
        // TODO

        return mapShapeObservation;
    }

    /**
     * Get all markers as an iterable
     *
     * @return iterable markers
     */
    public MapMarkerObservationIterable getMarkers() {
        return new MapMarkerObservationIterable();
    }

    /**
     * Get all shapes as an iterable
     *
     * @return iterable shapes
     */
    public MapShapeObservationIterable getShapes() {
        return new MapShapeObservationIterable();
    }

    /**
     * Iterable Map Marker Observations
     */
    public class MapMarkerObservationIterable extends MapObservationsIterable<MapMarkerObservation> {

        /**
         * Constructor
         */
        public MapMarkerObservationIterable() {
            super(MapMarkerObservation.class);
        }

    }

    /**
     * Iterable Map Shape Observations
     */
    public class MapShapeObservationIterable extends MapObservationsIterable<MapShapeObservation> {

        /**
         * Constructor
         */
        public MapShapeObservationIterable() {
            super(MapShapeObservation.class);
        }

    }

    /**
     * Iterable Map Observations of a specified type
     *
     * @param <T> map observation type
     */
    protected class MapObservationsIterable<T extends MapObservation> implements Iterable<T> {

        /**
         * Map Observation class type
         */
        private final Class<T> type;

        /**
         * Map Observation iterator
         */
        private final Iterator<MapObservation> mapObservationIterator;

        /**
         * Constructor
         *
         * @param type map observation class type
         */
        public MapObservationsIterable(Class<T> type) {
            this.type = type;
            mapObservationIterator = observationIds.values().iterator();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Iterator<T> iterator() {
            Iterator<T> iterator = new Iterator<T>() {

                private T next = null;

                /**
                 * {@inheritDoc}
                 */
                @Override
                public boolean hasNext() {
                    if (next == null) {
                        // Find the next map observation of the specified type
                        while (mapObservationIterator.hasNext()) {
                            MapObservation nextMapObservation = mapObservationIterator.next();
                            if (type.isAssignableFrom(nextMapObservation.getClass())) {
                                next = (T) nextMapObservation;
                                break;
                            }
                        }
                    }
                    return next != null;
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public T next() {
                    T nextValue = next;
                    next = null;
                    return nextValue;
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
            return iterator;
        }

    }

}
