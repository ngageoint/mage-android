package mil.nga.giat.mage.database.model.event;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.ArrayList;
import java.util.Collection;

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
	private String acl;

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private String style;

	@ForeignCollectionField
	private Collection<Form> forms = new ArrayList<>();
	
	public Event() {
		// ORMLite needs a no-arg constructor
	}

	public Event(String remoteId, String name, String description, String acl) {
		super();
		this.remoteId = remoteId;
		this.name = name;
		this.description = description;
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

	public Collection<Form> getForms() {
		return forms;
	}

	public void setForms(Collection<Form> forms) {
		this.forms = forms;
	}

	public JsonObject getAcl() {
		return JsonParser.parseString(acl).getAsJsonObject();
	}

	public String getStyle() {
		return style;
	}

	public void setStyle(String style) {
		this.style = style;
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
