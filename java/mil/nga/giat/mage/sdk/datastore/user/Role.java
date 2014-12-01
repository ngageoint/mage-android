package mil.nga.giat.mage.sdk.datastore.user;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "roles")
public class Role {

	@DatabaseField(generatedId = true)
	private Long pk_id;

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

	public Long getPk_id() {
		return pk_id;
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

}
