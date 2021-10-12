package mil.nga.giat.mage.sdk.datastore.user;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Join table for the many to many relationship between users and teams
 */
@DatabaseTable(tableName = "userteams")
public class UserTeam {
    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true)
    private User user;

    @DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true)
    private Team team;

    public UserTeam() {
        // ORMLite needs a no-arg constructor
    }

    public UserTeam(User user, Team team) {
        this.user = user;
        this.team = team;
    }
}