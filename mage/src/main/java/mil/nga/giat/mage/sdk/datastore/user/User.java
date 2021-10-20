package mil.nga.giat.mage.sdk.datastore.user;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@DatabaseTable(tableName = "users")
public class User {

	public static final String COLUMN_NAME_REMOTE_ID = "remote_id";
	public static final String COLUMN_NAME_USERNAME = "username";
	public static final String COLUMN_NAME_USER_LOCAL_ID = "user_local_id";
	public static final String COLUMN_NAME_LAST_MODIFIED = "last_modified";

	@DatabaseField(generatedId = true)
	private Long _id;

	@DatabaseField(unique = true, columnName = COLUMN_NAME_REMOTE_ID)
	private String remoteId;

	@DatabaseField
	private String email;

	@DatabaseField
	private String displayName;

	@DatabaseField(canBeNull = false, unique = true, columnName = COLUMN_NAME_USERNAME)
	private String username;

	@DatabaseField(canBeNull = false, columnName = "fetched_date")
	private Date fetchedDate = new Date(0);

	@DatabaseField(canBeNull = false, columnName = "last_modified")
	private Date lastModified = new Date(0);

	@DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true)
	private Role role;

	@DatabaseField
	private String primaryPhone;
	
	@DatabaseField
	private String avatarUrl;
	
	@DatabaseField
	private String iconUrl;

	@DatabaseField
	private String recentEventIds;

	@DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true, columnName = COLUMN_NAME_USER_LOCAL_ID)
	private UserLocal userLocal;

	public User() {
		// ORMLite needs a no-arg constructor
	}

	public User(String remoteId, String username, String displayName, String email, String primaryPhone, String avatarUrl, String iconUrl, String recentEventIds, Role role) {
		super();
		this.remoteId = remoteId;
		this.email = email;
		this.displayName = displayName;
		this.username = username;
		this.primaryPhone = primaryPhone;
		this.avatarUrl = avatarUrl;
		this.iconUrl = iconUrl;
		this.recentEventIds = recentEventIds;
		this.role = role;
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

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public Date getFetchedDate() {
		return fetchedDate;
	}

	public void setFetchedDate(Date fetchedDate) {
		this.fetchedDate = fetchedDate;
	}

	public Date getLastModified() {
		return lastModified;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}
	
	public String getPrimaryPhone() {
		return primaryPhone;
	}
	
	public void setPrimaryPhone(String primaryPhone) {
		this.primaryPhone = primaryPhone;
	}
	
	public String getAvatarUrl() {
		return avatarUrl;
	}
	
	public void setAvatarUrl(String avatarUrl) {
		this.avatarUrl = avatarUrl;
	}

	public String getIconUrl() {
		return iconUrl;
	}
	
	public void setIconUrl(String iconUrl) {
		this.iconUrl = iconUrl;
	}

	public List<String> getRecentEventIds() {
		return Arrays.asList(StringUtils.split(recentEventIds, ","));
	}

	public void setRecentEventIds(String recentEventIds) {
		this.recentEventIds = recentEventIds;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public UserLocal getUserLocal() {
		return userLocal;
	}

	public void setUserLocal(UserLocal userLocal) {
		this.userLocal = userLocal;
	}

	public boolean isCurrentUser() {
		return userLocal.isCurrentUser();
	}

	public Event getCurrentEvent() {
		return userLocal.getCurrentEvent();
	}

	public String getAvatarPath() {
		return userLocal.getLocalAvatarPath();
	}

	public String getIconPath() {
		return userLocal.getLocalIconPath();
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
        User other = (User) obj;
        return new EqualsBuilder().append(remoteId, other.remoteId).isEquals();
    }

}
