package mil.nga.giat.mage.sdk.datastore.observation;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import mil.nga.giat.mage.sdk.Temporal;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.utils.GeometryUtility;
import mil.nga.wkb.geom.Geometry;
import mil.nga.wkb.geom.GeometryType;
import mil.nga.wkb.geom.Point;
import mil.nga.wkb.util.GeometryUtils;

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

	@DatabaseField(columnName = "geometry", canBeNull = false, dataType = DataType.BYTE_ARRAY)
    private byte[] geometryBytes;

    @DatabaseField
    private String provider;

    @DatabaseField
    private Float accuracy;

    @DatabaseField
    private String locationDelta;

    @DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true)
    private Event event;

    @ForeignCollectionField(eager = true)
    private Collection<ObservationForm> forms = new ArrayList<>();

    @ForeignCollectionField(eager = true)
    private Collection<Attachment> attachments = new ArrayList<>();

    @DatabaseField(canBeNull = true, foreign = true, foreignAutoRefresh = true, foreignAutoCreate = true)
    private ObservationImportant important;

    @DatabaseField(persisterClass = ObservationErrorClassPersister.class, columnName = "error")
    private ObservationError error;

    @ForeignCollectionField(eager = true)
    private Collection<ObservationFavorite> favorites = new ArrayList<>();

    public Observation() {
        // ORMLite needs a no-arg constructor
    }

    public Observation(Geometry geometry, Collection<ObservationForm> forms, Collection<Attachment> attachments, Date timestamp, Event event) {
        this(null, null, geometry, forms, attachments, timestamp, event);
        this.dirty = true;
    }

    public Observation(String remoteId, Date lastModified, Geometry geometry, Collection<ObservationForm> forms, Collection<Attachment> attachments, Date timestamp, Event event) {
        super();
        this.remoteId = remoteId;
        this.lastModified = lastModified;
        this.geometryBytes = GeometryUtility.toGeometryBytes(geometry);
        this.forms = forms;
        this.attachments = attachments;
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

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Float accuracy) {
        this.accuracy = accuracy;
    }

    public String getLocationDelta() {
        return locationDelta;
    }

    public void setLocationDelta(String locationDelta) {
        this.locationDelta = locationDelta;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Float accuracy) {
        this.accuracy = accuracy;
    }

    public String getLocationDelta() {
        return locationDelta;
    }

    public void setLocationDelta(String locationDelta) {
        this.locationDelta = locationDelta;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public ObservationError getError() {
        return error;
    }

    public void setError(ObservationError error) {
        this.error = error;
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

    public Collection<ObservationForm> getForms() {
        return forms;
    }

    public void setForms(Collection<ObservationForm> forms) {
        this.forms = forms;
    }

    public void addForms(Collection<ObservationForm> forms) {

        Map<Long, ObservationForm> newFormsMap = new HashMap<Long, ObservationForm>();
        for (ObservationForm form : forms) {
            form.setObservation(this);
            newFormsMap.put(form.getFormId(), form);
        }

        Map<Long, ObservationForm> oldFormsMap = getFormsMap();

        oldFormsMap.putAll(newFormsMap);
        this.forms = oldFormsMap.values();
    }

    public Collection<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(Collection<Attachment> attachments) {
        this.attachments = attachments;
    }

    public ObservationImportant getImportant() {
        return important;
    }

    public void setImportant(ObservationImportant important) {
        this.important = important;
    }

    public Collection<ObservationFavorite> getFavorites() {
        return favorites;
    }

    public void setFavorites(Collection<ObservationFavorite> favorites) {
        this.favorites = favorites;
    }

    /**
     * A convenience method used for returning an Observation's properties in a
     * more useful data-structure.
     *
     * @return
     */
    public final Map<Long, ObservationForm> getFormsMap() {
        Map<Long, ObservationForm> formsMap = new HashMap<>();
        for (ObservationForm form : forms) {
            formsMap.put(form.getFormId(), form);
        }

        return formsMap;
    }

    /**
     * A convenience method used for returning an Observation's favorites in a
     * more useful data-structure.
     *
     * Map key is the userId who favorited the observation.
     *
     * @return
     */
    public final Map<String, ObservationFavorite> getFavoritesMap() {
        Map<String, ObservationFavorite> favoritesMap = new HashMap<>();
        for (ObservationFavorite favorite : favorites) {
            favoritesMap.put(favorite.getUserId(), favorite);
        }

        return favoritesMap;
    }

    public Uri getGoogleMapsUri() {
        DecimalFormat latLngFormat = new DecimalFormat("###.#####");
        String uriString = "http://maps.google.com/maps";

        Geometry geometry = getGeometry();
        Point point = GeometryUtils.getCentroid(geometry);
        uriString += String.format("?daddr=%1$s,%2$s",  latLngFormat.format(point.getY()), latLngFormat.format(point.getX()));

        return Uri.parse(uriString);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public int compareTo(@NonNull Observation another) {
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

	public ObservationProperty getPrimaryField() {
        return getField("primaryField");
    }

    public ObservationProperty getSecondaryField() {
        return getField("variantField");
    }

    public ObservationProperty getField(String name) {
        ObservationProperty field = null;
        Collection<ObservationForm> forms = getForms();
        if (forms != null && forms.size() > 0) {
            ObservationForm form = forms.iterator().next();
            JsonObject eventForm = getEventForm(form.getFormId());
            if (eventForm != null) {
                JsonElement fieldName = eventForm.get(name);
                if (fieldName != null && !fieldName.isJsonNull()) {
                    field = form.getPropertiesMap().get(fieldName.getAsString());
                }

            }
        }

        return field;
    }

    private JsonObject getEventForm(Long formId) {
        Iterator<JsonElement> iterator = event.getForms().iterator();
        while (iterator.hasNext()) {
            JsonObject formJson = (JsonObject) iterator.next();
            Long id = formJson.get("id").getAsLong();
            if (id.equals(formId)) {
                return formJson;
            }
        }

        return null;
    }


}