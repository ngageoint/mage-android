package mil.nga.giat.mage.sdk.datastore.location;

import androidx.annotation.NonNull;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import mil.nga.giat.mage.sdk.Temporal;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.utils.GeometryUtility;
import mil.nga.sf.Geometry;

@DatabaseTable(tableName = "locations")
public class Location implements Comparable<Location>, Temporal {

	public static final String COLUMN_NAME_USER_ID = "user_id";
	public static final String COLUMN_NAME_EVENT_ID = "event_id";
	public static final String COLUMN_NAME_TIMESTAMP = "timestamp";

	// name _id needed for cursor adapters
	@DatabaseField(generatedId = true)
	private Long _id;

	@DatabaseField(unique = true, columnName = "remote_id")
	private String remoteId;

	@DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true, columnName = COLUMN_NAME_USER_ID)
	private User user;

    /**
     * This is the time the location was reported at.  Time the GPS picked up.
     */
    @DatabaseField(canBeNull = false, dataType = DataType.DATE_LONG, columnName = COLUMN_NAME_TIMESTAMP)
    private Date timestamp = new Date(0);
    
    /**
     * This is the time the server created or updated the observation.
     */
    @DatabaseField(columnName = "last_modified", dataType = DataType.DATE_LONG)
    private Date lastModified = new Date(0);

	@DatabaseField
	private String type;

	@ForeignCollectionField(eager = true)
	private Collection<LocationProperty> properties  = new ArrayList<LocationProperty>();

	@DatabaseField(columnName = "geometry", canBeNull = false, dataType = DataType.BYTE_ARRAY)
	private byte[] geometryBytes;

	@DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true, columnName = COLUMN_NAME_EVENT_ID)
	private Event event;

	public Location() {
		// ORMLite needs a no-arg constructor
	}

	public Location(String type, User user, Collection<LocationProperty> properties, Geometry geometry, Date timestamp, Event event) {
		this(null, user, null, type, properties, geometry, timestamp, event);
	}

	public Location(String remoteId, User user, Date lastModified, String type, Collection<LocationProperty> properties, Geometry geometry, Date timestamp, Event event) {
		super();
		this.remoteId = remoteId;
		this.user = user;
		this.lastModified = lastModified;
		this.type = type;
		this.properties = properties;
		this.geometryBytes = GeometryUtility.toGeometryBytes(geometry);
		this.timestamp = timestamp;
		this.event = event;
	}

	public Long getId() {
		return _id;
	}

	public void setId(Long id) {
		this._id = id;
	}

	public String getRemoteId() {
		return remoteId;
	}

	public void setRemoteId(String remoteId) {
		this.remoteId = remoteId;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	@Override
	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Event getEvent() {
		return event;
	}

	public void setEvent(Event event) {
		this.event = event;
	}

	public Collection<LocationProperty> getProperties() {
		return properties;
	}

	public void setProperties(Collection<LocationProperty> properties) {
		this.properties = properties;
	}

	public byte[] getGeometryBytes() {
		return geometryBytes;
	}

	public void setGeometryBytes(byte[] geometryBytes) {
		this.geometryBytes = geometryBytes;
	}

	public Geometry getGeometry() {
		return GeometryUtility.toGeometry(getGeometryBytes());
	}

	public void setGeometry(Geometry geometry) {
		this.geometryBytes = GeometryUtility.toGeometryBytes(geometry);
	}
	
	/**
	 * A convenience method used for returning a Location's properties in a more useful data-structure.
	 * 
	 * @return
	 */
	public final Map<String, LocationProperty> getPropertiesMap() {
	     Map<String, LocationProperty> propertiesMap = new HashMap<String, LocationProperty>();
	        for (LocationProperty property : properties) {
	            propertiesMap.put(property.getKey(), property);
	        }

	        return propertiesMap;
	    }

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	@Override
	public int compareTo(@NonNull Location another) {
		return new CompareToBuilder().append(this._id, another._id).append(this.remoteId, another.remoteId).toComparison();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_id == null) ? 0 : _id.hashCode());
		result = prime * result + ((remoteId == null) ? 0 : remoteId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Location other = (Location) obj;
		return new EqualsBuilder().append(_id, other._id).append(remoteId, other.remoteId).isEquals();
	}
}
