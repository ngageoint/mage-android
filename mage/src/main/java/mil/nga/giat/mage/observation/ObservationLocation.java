package mil.nga.giat.mage.observation;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;

import mil.nga.giat.mage.sdk.utils.GeometryUtility;
import mil.nga.wkb.geom.Geometry;
import mil.nga.wkb.geom.GeometryType;
import mil.nga.wkb.geom.Point;

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
    public Point getPoint() {
        // TODO get the first point from any geometry?
        Point point = null;
        if (geometry.getGeometryType() == GeometryType.POINT) {
            point = (Point) geometry;
        }
        return point;
    }

    /**
     * Get the first point from the geometry as a {@link LatLng}
     *
     * @return lat lng
     */
    public LatLng getLatLng() {
        Point point = getPoint();
        LatLng latLng = new LatLng(point.getY(), point.getX());
        return latLng;
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

}
