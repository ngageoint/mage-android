package mil.nga.giat.mage.sdk.datastore.observation;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Date;

@DatabaseTable(tableName = "observation_important")
public class ObservationImportant {

    @DatabaseField(generatedId = true, columnName="pk_id")
    private Long id;

    @DatabaseField(columnName="user_id")
    private String userId;

    @DatabaseField(canBeNull = false, dataType = DataType.DATE_LONG)
    private Date timestamp = new Date(0);

    @DatabaseField(columnName="description")
    private String description;

    @DatabaseField(canBeNull = false, columnName="is_important")
    private boolean important;

    @DatabaseField(canBeNull = false, columnName = "dirty")
    private boolean dirty = Boolean.TRUE;

    public ObservationImportant() {
        // ORMLite needs a no-arg constructor
    }

    public ObservationImportant(String userId, boolean important) {
        this.userId = userId;
        this.important = important;
    }

    public ObservationImportant(String userId) {
        this.userId = userId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isImportant() {
        return important;
    }

    public void setImportant(boolean important) {
        this.important = important;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
