package mil.nga.giat.mage.sdk.datastore.user;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Date;

@DatabaseTable(tableName = "users")
public class User {

	@DatabaseField(generatedId = true)
	private Long _id;

	@DatabaseField(unique = true, columnName = "remote_id")
	private String remoteId;

	@DatabaseField
	private String email;

	@DatabaseField
	private String displayName;

	@DatabaseField(canBeNull = false, unique = true)
	private String username;

	@DatabaseField(canBeNull = false)
	private Boolean isCurrentUser = Boolean.FALSE;

	@DatabaseField(canBeNull = false, columnName = "fetched_date")
	private Date fetchedDate = new Date(0);

	@DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true)
	private Role role;

    @DatabaseField(canBeNull = true, foreign = true, foreignAutoRefresh = true)
    private Event currentEvent;

	@DatabaseField
	private String primaryPhone;
	
	@DatabaseField
	private String avatarUrl;
	
	@DatabaseField
	private String localAvatarPath;
	
	@DatabaseField
	private String localIconPath;
	
	@DatabaseField
	private String iconUrl;

	public User() {
		// ORMLite needs a no-arg constructor
	}

	public User(String remoteId, String email, String displayName, String username, Role role, Event currentEvent, String primaryPhone, String avatarUrl, String iconUrl) {
		super();
		this.remoteId = remoteId;
		this.email = email;
		this.displayName = displayName;
		this.username = username;
		this.role = role;
        this.currentEvent = currentEvent;
		this.primaryPhone = primaryPhone;
		this.avatarUrl = avatarUrl;
		this.iconUrl = iconUrl;
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

	public String getDisplayName() {
		return displayName;
	}

	public String getUsername() {
		return username;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
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

	public Date getFetchedDate() {
		return fetchedDate;
	}

	public void setFetchedDate(Date fetchedDate) {
		this.fetchedDate = fetchedDate;
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
	
	public String getLocalAvatarPath() {
		return localAvatarPath;
	}
	
	public void setLocalAvatarPath(String localAvatarPath) {
		this.localAvatarPath = localAvatarPath;
	}
	
	public String getIconUrl() {
		return iconUrl;
	}
	
	public void setIconUrl(String iconUrl) {
		this.iconUrl = iconUrl;
	}
	
	public String getLocalIconPath() {
		return localIconPath;
	}
	
	public void setLocalIconPath(String localIconPath) {
		this.localIconPath = localIconPath;
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
