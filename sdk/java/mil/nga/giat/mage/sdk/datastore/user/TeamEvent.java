package mil.nga.giat.mage.sdk.datastore.user;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Join table for the many to many relationship between teams and events
 */
@DatabaseTable(tableName = "teamevents")
public class TeamEvent {
    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true)
    private Team team;

    @DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true)
    private Event event;

    public TeamEvent() {
        // ORMLite needs a no-arg constructor
    }

    public TeamEvent(Team team, Event event) {
        this.team = team;
        this.event = event;
    }
}