package mil.nga.giat.mage.sdk.datastore.observation;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import org.apache.commons.lang3.builder.ToStringBuilder;

@DatabaseTable(tableName = "observation_favorites")
public class ObservationFavorite {

    @DatabaseField(generatedId = true, columnName="pk_id")
    private Long id;

    @DatabaseField(columnName="user_id")
    private String userId;

    @DatabaseField(canBeNull = false, columnName="is_favorite")
    private boolean favorite;

    @DatabaseField(canBeNull = false, columnName = "dirty")
    private boolean dirty = Boolean.TRUE;

    @DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true)
    private Observation observation;

    public ObservationFavorite() {
        // ORMLite needs a no-arg constructor
    }

    public ObservationFavorite(String userId, boolean favorite) {
        this.userId = userId;
        this.favorite = favorite;
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

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public Observation getObservation() {
        return observation;
    }

    public void setObservation(Observation observation) {
        this.observation = observation;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
