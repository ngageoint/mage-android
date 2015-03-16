package mil.nga.giat.mage.sdk.datastore.observation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import mil.nga.giat.mage.sdk.Temporal;
import mil.nga.giat.mage.sdk.datastore.user.Event;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;
import com.vividsolutions.jts.geom.Geometry;

@DatabaseTable(tableName = "observations")
public class Observation implements Comparable<Observation>, Temporal {
    
    // name _id needed for cursor adapters
    @DatabaseField(generatedId = true)
    private Long _id;

    @DatabaseField(unique = true, columnName = "remote_id")
    private String remoteId;

    @DatabaseField(unique = true, columnName = "url")
    private String url;

	/**
	 * This is really the remote id!
	 */
	@DatabaseField(columnName = "user_id", canBeNull = false)
	private String userId = "-1";

    @DatabaseField(columnName = "device_id")
    private String deviceId;
    
    /**
     * This is the time the server created or updated the observation.  It can also be a local time, if no time from the server is given.
     */
    @DatabaseField(canBeNull = false, columnName = "last_modified", dataType = DataType.DATE_LONG)
    private Date lastModified = null;
    
    /**
     * This is the time the observation was made/reported at.
     */
    @DatabaseField(canBeNull = false, dataType = DataType.DATE_LONG)
    private Date timestamp = new Date(0);

    @DatabaseField(canBeNull = false)
    private boolean dirty = Boolean.TRUE;

    @DatabaseField(canBeNull = false)
    private State state = State.ACTIVE;

	@DatabaseField(canBeNull = false, dataType = DataType.SERIALIZABLE)
	private Geometry geometry;

    @DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true)
    private Event event;

    @ForeignCollectionField(eager = true)
    private Collection<ObservationProperty> properties = new ArrayList<ObservationProperty>();

    @ForeignCollectionField(eager = true)
    private Collection<Attachment> attachments = new ArrayList<Attachment>();

    public Observation() {
        // ORMLite needs a no-arg constructor
    }

    public Observation(Geometry geometry, Collection<ObservationProperty> pProperties, Collection<Attachment> pAttachments, Date timestamp, Event event) {
        this(null, null, geometry, pProperties, pAttachments, timestamp, event);
        this.dirty = true;
    }

    public Observation(String remoteId, Date lastModified, Geometry geometry, Collection<ObservationProperty> pProperties, Collection<Attachment> pAttachments, Date timestamp, Event event) {
        super();
        this.remoteId = remoteId;
        this.lastModified = lastModified;
        this.geometry = geometry;
        this.properties = pProperties;
        this.attachments = pAttachments;
        this.dirty = false;
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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

	public Geometry getGeometry() {
		return geometry;
	}

	public void setGeometry(Geometry geometry) {
		this.geometry = geometry;
	}

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public Date getLastModified() {
        return lastModified;
    }
    
    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public boolean isDirty() {
        return dirty;
    }
    
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public Collection<ObservationProperty> getProperties() {
        return properties;
    }

    public void setProperties(Collection<ObservationProperty> properties) {
        this.properties = properties;
    }

    public void addProperties(Collection<ObservationProperty> properties) {

        Map<String, ObservationProperty> newPropertiesMap = new HashMap<String, ObservationProperty>();
        for (ObservationProperty property : properties) {
            property.setObservation(this);
            newPropertiesMap.put(property.getKey(), property);
        }

        Map<String, ObservationProperty> oldPropertiesMap = getPropertiesMap();

        oldPropertiesMap.putAll(newPropertiesMap);
        this.properties = oldPropertiesMap.values();
    }

    public Collection<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(Collection<Attachment> attachments) {
        this.attachments = attachments;
    }

    /**
     * A convenience method used for returning an Observation's properties in a
     * more useful data-structure.
     * 
     * @return
     */
    public final Map<String, ObservationProperty> getPropertiesMap() {
        Map<String, ObservationProperty> propertiesMap = new HashMap<String, ObservationProperty>();
        for (ObservationProperty property : properties) {
            propertiesMap.put(property.getKey(), property);
        }

        return propertiesMap;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public int compareTo(Observation another) {
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
        Observation other = (Observation) obj;
        return new EqualsBuilder().append(_id, other._id).append(remoteId, other.remoteId).isEquals();
    }

	@Override
	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
}