package mil.nga.giat.mage.sdk.datastore.user;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "userlocal")
public class UserLocal {

	public static final String COLUMN_NAME_ID = "id";
	public static final String COLUMN_NAME_CURRENT_USER = "current_user";
	public static final String COLUMN_NAME_CURRENT_EVENT = "current_event";
	public static final String COLUMN_NAME_AVATAR_PATH = "avatar_path";
	public static final String COLUMN_NAME_ICON_PATH = "icon_path";

	@DatabaseField(generatedId = true, columnName = COLUMN_NAME_ID)
	private Long id;

	@DatabaseField(canBeNull = false, columnName = COLUMN_NAME_CURRENT_USER)
	private Boolean isCurrentUser = Boolean.FALSE;

    @DatabaseField(canBeNull = true, foreign = true, foreignAutoRefresh = true, columnName = COLUMN_NAME_CURRENT_EVENT)
    private Event currentEvent;

	@DatabaseField(columnName = COLUMN_NAME_AVATAR_PATH)
	private String localAvatarPath;

	@DatabaseField(columnName = COLUMN_NAME_ICON_PATH)
	private String localIconPath;

	public UserLocal() {
		// ORMLite needs a no-arg constructor
	}

	public Long getId() {
		return id;
	}

	public void setId(Long _id) {
		this.id = _id;
	}

    public Event getCurrentEvent() {
        return currentEvent;
    }

    public void setCurrentEvent(Event currentEvent) {
        this.currentEvent = currentEvent;
    }

	public Boolean isCurrentUser() {
		return isCurrentUser;
	}

	public void setCurrentUser(Boolean isCurrentUser) {
		this.isCurrentUser = isCurrentUser;
	}

	public String getLocalAvatarPath() {
		return localAvatarPath;
	}
	
	public void setLocalAvatarPath(String localAvatarPath) {
		this.localAvatarPath = localAvatarPath;
	}

	public String getLocalIconPath() {
		return localIconPath;
	}
	
	public void setLocalIconPath(String localIconPath) {
		this.localIconPath = localIconPath;
	}
}
