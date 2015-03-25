package mil.nga.giat.mage.sdk.datastore.observation;

import android.os.Parcel;
import android.os.Parcelable;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

@DatabaseTable(tableName = "attachments")
public class Attachment implements Parcelable {

	@DatabaseField(generatedId = true, columnName="pk_id")
	private Long id;

	@DatabaseField(unique = true, columnName="remote_id")
	private String remoteId;

	@DatabaseField(columnName="content_type")
	private String contentType;

	@DatabaseField(columnName="size")
	private Long size;

	@DatabaseField(columnName="name")
	private String name;

	@DatabaseField(columnName="local_path")
	private String localPath;

	@DatabaseField(columnName="remote_path")
	private String remotePath;
	
	@DatabaseField(columnName="url")
	private String url;
	
	@DatabaseField(canBeNull = false)
	private boolean dirty = Boolean.TRUE;

	@DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true)
	private Observation observation;

	public Attachment() {
		// ORMLite needs a no-arg constructor
	}

	public Attachment(String contentType, Long size, String name, String localPath, String remotePath) {
		this.contentType = contentType;
		this.size = size;
		this.name = name;
		this.localPath = localPath;
		this.remotePath = remotePath;
		this.setDirty(true);
	}

	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}

	public String getRemoteId() {
		return remoteId;
	}

	public void setRemoteId(String remoteId) {
		this.remoteId = remoteId;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public Long getSize() {
		return size;
	}

	public void setSize(Long size) {
		this.size = size;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLocalPath() {
		return localPath;
	}

	public void setLocalPath(String localPath) {
		this.localPath = localPath;
	}

	public String getRemotePath() {
		return remotePath;
	}

	public void setRemotePath(String remotePath) {
		this.remotePath = remotePath;
	}
	
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
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
	
	// Parcelable stuff
	public Attachment(Parcel in) {
		id = (Long)in.readValue(Long.class.getClassLoader());
		remoteId = in.readString();
		contentType = in.readString();
		size = (Long)in.readValue(Long.class.getClassLoader());
		name = in.readString();
		localPath = in.readString();
		remotePath = in.readString();
		url = in.readString();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeValue(id);
		out.writeString(remoteId);
		out.writeString(contentType);
		out.writeValue(size);
		out.writeString(name);
		out.writeString(localPath);
		out.writeString(remotePath);
		out.writeString(url);
	}
	
	public static final Parcelable.Creator<Attachment> CREATOR = new Parcelable.Creator<Attachment>() {
	      public Attachment createFromParcel(Parcel source) {
	            return new Attachment(source);
	      }
	      public Attachment[] newArray(int size) {
	            return new Attachment[size];
	      }
	};
	
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((remoteId == null) ? 0 : remoteId.hashCode());
        result = prime * result + ((localPath == null) ? 0 : localPath.hashCode());
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
        Attachment other = (Attachment) obj;
        return new EqualsBuilder().append(id, other.id).append(remoteId, other.remoteId).append(localPath, other.localPath).isEquals();
    }
}
