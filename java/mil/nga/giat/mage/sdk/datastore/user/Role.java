package mil.nga.giat.mage.sdk.datastore.user;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "roles")
public class Role {

	@DatabaseField(generatedId = true)
	private Long _id;

	@DatabaseField(unique = true, columnName = "remote_id")
	private String remoteId;

	@DatabaseField(canBeNull = false)
	private String name;

	@DatabaseField
	private String description;

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private Permissions permissions = new Permissions();

	public Role() {
		// ORMLite needs a no-arg constructor
	}

	public Role(String remoteId, String name, String description, Permissions permissions) {
		super();
		this.remoteId = remoteId;
		this.name = name;
		this.description = description;
		this.permissions = permissions;
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

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Permissions getPermissions() {
		return permissions;
	}

	public void setPermissions(Permissions permissions) {
		this.permissions = permissions;
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
        Role other = (Role) obj;
        return new EqualsBuilder().append(remoteId, other.remoteId).isEquals();
    }

}
