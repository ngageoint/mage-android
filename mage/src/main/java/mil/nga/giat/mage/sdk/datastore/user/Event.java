package mil.nga.giat.mage.sdk.datastore.user;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@DatabaseTable(tableName = "events")
public class Event {

	public static final String COLUMN_NAME_REMOTE_ID = "remote_id";

	@DatabaseField(generatedId = true)
	private Long _id;

	@DatabaseField(unique = true, columnName = COLUMN_NAME_REMOTE_ID)
	private String remoteId;

	@DatabaseField(canBeNull = false)
	private String name;

    @DatabaseField
    private String description;

	@DatabaseField
	private Integer minObservationForms;

	@DatabaseField
	private Integer maxObservationForms;

    @DatabaseField
	private String forms;

	@DatabaseField
	private String acl;
	
	public Event() {
		// ORMLite needs a no-arg constructor
	}

	public Event(String remoteId, String name, String description, String forms, String acl) {
		super();
		this.remoteId = remoteId;
		this.name = name;
		this.description = description;
		this.forms = forms;
		this.acl = acl;
	}

	public Long getId() {
		return _id;
	}

    public void setId(Long _id) {
        this._id = _id;
    }

	public String getRemoteId() {
		return remoteId;
	}

	public void setRemoteId(String remoteId) {
		this.remoteId = remoteId;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public Integer getMinObservationForms() {
		return minObservationForms;
	}

	public void setMinObservationForms(Integer minObservationForms) {
		this.minObservationForms = minObservationForms;
	}

	public Integer getMaxObservationForms() {
		return maxObservationForms;
	}

	public void setMaxObservationForms(Integer maxObservationForms) {
		this.maxObservationForms = maxObservationForms;
	}

	public JsonArray getForms() {
        return new JsonParser().parse(forms).getAsJsonArray();
	}

	public JsonArray getNonArchivedForms() {
		JsonArray jsonForms = getForms();
		Iterator<JsonElement> iterator = jsonForms.iterator();

		while (iterator.hasNext()) {
			JsonObject jsonForm = iterator.next().getAsJsonObject();
			if (jsonForm.has("archived") && jsonForm.get("archived").getAsBoolean()) {
				iterator.remove();
			}
		}

		return jsonForms;
	}

	public Map<Long, JsonObject> getFormMap() {
		Map<Long, JsonObject> formMap = new HashMap<>();
		Iterator<JsonElement> iterator = new JsonParser().parse(forms).getAsJsonArray().iterator();
		while (iterator.hasNext()) {
			JsonObject form = (JsonObject) iterator.next();
			formMap.put(form.get("id").getAsLong(), form);
		}

		return formMap;
	}

	public JsonObject getAcl() {
		return new JsonParser().parse(acl).getAsJsonObject();
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
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
        Event other = (Event) obj;
        return new EqualsBuilder().append(remoteId, other.remoteId).isEquals();
    }

}
