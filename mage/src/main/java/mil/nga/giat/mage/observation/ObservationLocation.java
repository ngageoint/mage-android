package mil.nga.giat.mage.observation;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.List;

import mil.nga.geopackage.projection.ProjectionConstants;
import mil.nga.giat.mage.sdk.utils.GeometryUtility;
import mil.nga.wkb.geom.CompoundCurve;
import mil.nga.wkb.geom.Geometry;
import mil.nga.wkb.geom.GeometryCollection;
import mil.nga.wkb.geom.GeometryEnvelope;
import mil.nga.wkb.geom.GeometryType;
import mil.nga.wkb.geom.LineString;
import mil.nga.wkb.geom.MultiLineString;
import mil.nga.wkb.geom.MultiPoint;
import mil.nga.wkb.geom.MultiPolygon;
import mil.nga.wkb.geom.Point;
import mil.nga.wkb.geom.Polygon;
import mil.nga.wkb.geom.PolyhedralSurface;
import mil.nga.wkb.util.GeometryEnvelopeBuilder;
import mil.nga.wkb.util.GeometryUtils;

/**
 * Observation location containing the geometry and location collection information
 *
 * @author osbornb
 */
public class ObservationLocation implements Parcelable {

    /**
     * Provider for manually set locations
     */
    public static final String MANUAL_PROVIDER = "manual";

    /**
     * Default camera single point zoom level
     */
    public static final int DEFAULT_POINT_ZOOM = 17;

    /**
     * Default camera padding percentage when building a camera update for geometry zoom
     */
    public static final float DEFAULT_PADDING_PERCENTAGE = .18f;

    /**
     * Parcelable CREATOR implementation
     */
    public static final Parcelable.Creator<ObservationLocation> CREATOR = new Parcelable.Creator<ObservationLocation>() {
        public ObservationLocation createFromParcel(Parcel source) {
            return new ObservationLocation(source);
        }

        public ObservationLocation[] newArray(int size) {
            return new ObservationLocation[size];
        }
    };

    /**
     * Location geometry such as a point, linestring, polygon, etc
     */
    private Geometry geometry;

    /**
     * {@link Location#getAccuracy()}
     */
    private float accuracy;

    /**
     * {@link Location#getProvider()} or {@link #MANUAL_PROVIDER}
     */
    private String provider;

    /**
     * {@link Location#getTime()}
     */
    private long time = 0;

    /**
     * {@link Location#getElapsedRealtimeNanos()}
     */
    private long elapsedRealtimeNanos = 0;

    /**
     * Default constructor
     */
    public ObservationLocation() {
    }

    /**
     * Constructor with Android Location
     *
     * @param location collected location
     */
    public ObservationLocation(Location location) {
        setGeometry(location);
        setAccuracy(location.getAccuracy());
        setProvider(location.getProvider());
        setTime(location.getTime());
        setElapsedRealtimeNanos(location.getElapsedRealtimeNanos());
    }

    /**
     * Constructor with geometry
     *
     * @param geometry geometry
     */
    public ObservationLocation(Geometry geometry) {
        this(null, geometry);
    }

    /**
     * Constructor with specified provider and geometry
     *
     * @param provider provider
     * @param geometry geometry
     */
    public ObservationLocation(String provider, Geometry geometry) {
        setProvider(provider);
        setGeometry(geometry);
    }

    /**
     * Constructor with {@link LatLng}
     *
     * @param latLng lat lng point
     */
    public ObservationLocation(LatLng latLng) {
        this(null, latLng);
    }

    /**
     * Constructor with specified provider and {@link LatLng}
     *
     * @param provider provider
     * @param latLng   lat lng point
     */
    public ObservationLocation(String provider, LatLng latLng) {
        this(provider, new Point(latLng.longitude, latLng.latitude));
    }

    /**
     * Constructor to copy an existing Observation Location
     *
     * @param location observation location to copy
     */
    public ObservationLocation(ObservationLocation location) {
        setGeometry(location.getGeometry());
        setAccuracy(location.getAccuracy());
        setProvider(location.getProvider());
        setTime(location.getTime());
        setElapsedRealtimeNanos(location.getElapsedRealtimeNanos());
    }

    /**
     * Constructor for Parcelable implementation
     *
     * @param in parecel object
     */
    public ObservationLocation(Parcel in) {
        byte[] geometryBytes = new byte[in.readInt()];
        in.readByteArray(geometryBytes);
        geometry = GeometryUtility.toGeometry(geometryBytes);
        accuracy = in.readFloat();
        provider = in.readString();
        time = in.readLong();
        elapsedRealtimeNanos = in.readLong();
    }

    /**
     * Get the geometry
     *
     * @return geometry
     */
    public Geometry getGeometry() {
        return geometry;
    }

    /**
     * Set the geometry by Android location
     *
     * @param location collected location
     */
    public void setGeometry(Location location) {
        this.geometry = new Point(location.getLongitude(), location.getLatitude());
    }

    /**
     * Set the geometry
     *
     * @param geometry geometry
     */
    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    /**
     * Get the accuracy
     *
     * @return accuracy
     */
    public float getAccuracy() {
        return accuracy;
    }

    /**
     * Set the accuracy
     *
     * @param accuracy accuracy
     */
    public void setAccuracy(float accuracy) {
        this.accuracy = accuracy;
    }

    /**
     * Get the provider
     *
     * @return provider
     */
    public String getProvider() {
        return provider;
    }

    /**
     * Set the provider
     *
     * @param provider provider
     */
    public void setProvider(String provider) {
        this.provider = provider;
    }

    /**
     * Get the time
     *
     * @return time
     */
    public long getTime() {
        return time;
    }

    /**
     * Set the time
     *
     * @param time time
     */
    public void setTime(long time) {
        this.time = time;
    }

    /**
     * Get the elapsed realtime nanos time
     *
     * @return elapsed realtime nanos time
     */
    public long getElapsedRealtimeNanos() {
        return elapsedRealtimeNanos;
    }

    /**
     * Set the elapsed realtime nanos time
     *
     * @param elapsedRealtimeNanos elapsed realtime nanos time
     */
    public void setElapsedRealtimeNanos(long elapsedRealtimeNanos) {
        this.elapsedRealtimeNanos = elapsedRealtimeNanos;
    }

    /**
     * Get the first point from the geometry
     *
     * @return point
     */
    public Point getFirstPoint() {
        return getFirstPoint(geometry);
    }

    /**
     * Get the first point in the geometry
     *
     * @param geometry geometry
     * @return first point
     */
    private Point getFirstPoint(Geometry geometry) {

        Point point = null;

        GeometryType type = geometry.getGeometryType();
        switch (type) {
            case POINT:
                point = (Point) geometry;
                break;
            case MULTIPOINT:
                point = ((MultiPoint) geometry).getPoints().get(0);
                break;
            case LINESTRING:
            case CIRCULARSTRING:
                point = ((LineString) geometry).getPoints().get(0);
                break;
            case MULTILINESTRING:
                point = getFirstPoint(((MultiLineString) geometry).getLineStrings().get(0));
                break;
            case COMPOUNDCURVE:
                point = getFirstPoint(((CompoundCurve) geometry).getLineStrings().get(0));
                break;
            case POLYGON:
            case TRIANGLE:
                point = getFirstPoint(((Polygon) geometry).getRings().get(0));
                break;
            case MULTIPOLYGON:
                point = getFirstPoint(((MultiPolygon) geometry).getPolygons().get(0));
                break;
            case POLYHEDRALSURFACE:
            case TIN:
                point = getFirstPoint(((PolyhedralSurface) geometry).getPolygons().get(0));
                break;
            case GEOMETRYCOLLECTION:
                @SuppressWarnings("unchecked")
                GeometryCollection<Geometry> geomCollection = (GeometryCollection<Geometry>) geometry;
                point = getFirstPoint(geomCollection.getGeometries().get(0));
                break;
            default:
                Log.e(this.getClass().getSimpleName(), "Unsupported Geometry Type: " + type);
        }

        return point;
    }

    /**
     * Get the first point from the geometry as a {@link LatLng}
     *
     * @return lat lng
     */
    public LatLng getFirstLatLng() {
        Point point = getFirstPoint();
        LatLng latLng = new LatLng(point.getY(), point.getX());
        return latLng;
    }

    /**
     * Get the geometry centroid
     *
     * @return centroid point
     */
    public Point getCentroid() {
        return GeometryUtils.getCentroid(geometry);
    }

    /**
     * Get the geometry centroid as a LatLng
     *
     * @return centroid point lat lng
     */
    public LatLng getCentroidLatLng() {
        Point point = GeometryUtils.getCentroid(geometry);
        LatLng latLng = new LatLng(point.getY(), point.getX());
        return latLng;
    }

    /**
     * Get a geometry envelope that includes the entire geometry
     *
     * @return geometry envelope
     */
    public GeometryEnvelope getGeometryEnvelope() {
        Geometry geometryCopy = geometry.copy();
        GeometryUtils.minimizeGeometry(geometryCopy, ProjectionConstants.WGS84_HALF_WORLD_LON_WIDTH);
        return GeometryEnvelopeBuilder.buildEnvelope(geometryCopy);
    }

    /**
     * Get the camera update for zooming to the geometry
     *
     * @return camera update
     */
    public CameraUpdate getCameraUpdate() {
        return getCameraUpdate(null);
    }

    /**
     * Get the camera update for zooming to the geometry
     *
     * @param view view
     * @return camera update
     */
    public CameraUpdate getCameraUpdate(View view) {
        return getCameraUpdate(view, DEFAULT_POINT_ZOOM, DEFAULT_PADDING_PERCENTAGE);
    }

    /**
     * Get the camera update for zooming to the geometry
     *
     * @param view      view
     * @param pointZoom point zoom
     * @return camera update
     */
    public CameraUpdate getCameraUpdate(View view, int pointZoom) {
        return getCameraUpdate(view, pointZoom, DEFAULT_PADDING_PERCENTAGE);
    }

    /**
     * Get the camera update for zooming to the geometry
     *
     * @param pointZoom point zoom
     * @return camera update
     */
    public CameraUpdate getCameraUpdate(int pointZoom) {
        return getCameraUpdate(null, pointZoom);
    }

    /**
     * Get the camera update for zooming to the geometry
     *
     * @param view              view
     * @param paddingPercentage padding percentage
     * @return camera update
     */
    public CameraUpdate getCameraUpdate(View view, float paddingPercentage) {
        return getCameraUpdate(view, DEFAULT_POINT_ZOOM, paddingPercentage);
    }

    /**
     * Get the camera update for zooming to the geometry
     *
     * @param view              view
     * @param pointZoom         point zoom
     * @param paddingPercentage padding percentage
     * @return camera update
     */
    public CameraUpdate getCameraUpdate(View view, int pointZoom, float paddingPercentage) {

        CameraUpdate update = null;

        if (geometry.getGeometryType() == GeometryType.POINT) {

            update = CameraUpdateFactory.newLatLngZoom(getFirstLatLng(), pointZoom);

        } else {

            GeometryEnvelope envelope = getGeometryEnvelope();

            final LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
            boundsBuilder.include(new LatLng(envelope.getMinY(), envelope.getMinX()));
            boundsBuilder.include(new LatLng(envelope.getMinY(), envelope.getMaxX()));
            boundsBuilder.include(new LatLng(envelope.getMaxY(), envelope.getMinX()));
            boundsBuilder.include(new LatLng(envelope.getMaxY(), envelope.getMaxX()));

            int padding = 0;
            if (view != null) {
                int minViewLength = Math.min(view.getWidth(), view.getHeight());
                padding = (int) Math.floor(minViewLength
                        * paddingPercentage);
            }

            update = CameraUpdateFactory.newLatLngBounds(
                    boundsBuilder.build(), padding);

        }

        return update;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeToParcel(Parcel out, int flags) {
        byte[] geometryBytes = GeometryUtility.toGeometryBytes(geometry);
        out.writeInt(geometryBytes.length);
        out.writeByteArray(geometryBytes);
        out.writeFloat(accuracy);
        out.writeString(provider);
        out.writeLong(time);
        out.writeLong(elapsedRealtimeNanos);
    }

    /**
     * Check if the points form a rectangle
     *
     * @param points points
     * @return true if a rectangle
     */
    public static boolean checkIfRectangle(List<Point> points) {
        return checkIfRectangleAndFindSide(points) != null;
    }

    /**
     * Check if the points form a rectangle and return if the side one has the same x
     *
     * @param points points
     * @return null if not a rectangle, true if same x side 1, false if same y side 1
     */
    public static Boolean checkIfRectangleAndFindSide(List<Point> points) {
        Boolean sameXSide1 = null;
        int size = points.size();
        if (size == 4 || size == 5) {
            Point point1 = points.get(0);
            Point lastPoint = points.get(points.size() - 1);
            boolean closed = point1.getX() == lastPoint.getX() && point1.getY() == lastPoint.getY();
            if ((closed && size == 5) || (!closed && size == 4)) {
                Point point2 = points.get(1);
                Point point3 = points.get(2);
                Point point4 = points.get(3);
                if (point1.getX() == point2.getX() && point2.getY() == point3.getY()) {
                    if (point1.getY() == point4.getY() && point3.getX() == point4.getX()) {
                        sameXSide1 = true;
                    }
                } else if (point1.getY() == point2.getY() && point2.getX() == point3.getX()) {
                    if (point1.getX() == point4.getX() && point3.getY() == point4.getY()) {
                        sameXSide1 = false;
                    }
                }
            }
        }
        return sameXSide1;
    }

}
