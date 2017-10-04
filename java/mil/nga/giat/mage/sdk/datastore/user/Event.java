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
    private String forms;
	
	public Event() {
		// ORMLite needs a no-arg constructor
	}

	public Event(String remoteId, String name, String description, String forms) {
		super();
		this.remoteId = remoteId;
		this.name = name;
		this.description = description;
		this.forms = forms;
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

	public JsonArray getForms() {
        return new JsonParser().parse(forms).getAsJsonArray();
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
