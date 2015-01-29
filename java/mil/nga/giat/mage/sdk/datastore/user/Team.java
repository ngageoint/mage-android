package mil.nga.giat.mage.sdk.datastore.user;

import java.util.Date;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "teams")
public class Team {

    @DatabaseField(generatedId = true)
    private Long _id;

    @DatabaseField(unique = true, columnName = "remote_id")
    private String remoteId;

    @DatabaseField(canBeNull = false)
    private String name;

    @DatabaseField
    private String description;
	
	public Team() {
		// ORMLite needs a no-arg constructor
	}

	public Team(String remoteId, String name, String description) {
		super();
        this.remoteId = remoteId;
        this.name = name;
        this.description = description;
	}

    public Long getId() {
        return _id;
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

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

}
