package mil.nga.giat.mage.sdk.datastore.user;

import java.util.Date;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "events")
public class Event {

	@DatabaseField(generatedId = true)
	private Long _id;

	@DatabaseField(unique = true, columnName = "remote_id")
	private String remoteId;

	@DatabaseField(canBeNull = false)
	private String name;

    @DatabaseField
    private String description;

    @DatabaseField
    private String form;
	
	public Event() {
		// ORMLite needs a no-arg constructor
	}

	public Event(String remoteId, String name, String description, String form) {
		super();
		this.remoteId = remoteId;
		this.name = name;
		this.description = description;
		this.form = form;
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

	public JsonObject getForm() {
        return new JsonParser().parse(form).getAsJsonObject();
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

}
