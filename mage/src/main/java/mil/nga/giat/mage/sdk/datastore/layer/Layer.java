package mil.nga.giat.mage.sdk.datastore.layer;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.ArrayList;
import java.util.Collection;

import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeature;
import mil.nga.giat.mage.sdk.datastore.user.Event;

@DatabaseTable(tableName = "layers")
public class Layer implements Comparable<Layer> {

	@DatabaseField(generatedId = true)
	private Long id;

	@DatabaseField(canBeNull = false, unique = true, columnName = "remote_id")
	private String remoteId;

	@DatabaseField
	private String type;

	@DatabaseField
	private String name;

	@DatabaseField(columnName = "download_id")
	private Long downloadId;

	@DatabaseField(canBeNull = false)
	private boolean loaded = Boolean.FALSE;

	@DatabaseField(columnName = "relative_path")
	private String relativePath;

	@DatabaseField
	private String fileName;

	@DatabaseField
	private Long fileSize;

	@DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true)
	private Event event;

	@ForeignCollectionField(eager = false)
	private final Collection<StaticFeature> staticFeatures = new ArrayList<>();

	@DatabaseField
	private String url;

	@DatabaseField
	private String format;

	@DatabaseField
	private String wmsFormat;

	@DatabaseField
	private String wmsVersion;

	@DatabaseField
	private String wmsLayers;

	@DatabaseField
	private String wmsStyles;

	@DatabaseField
	private String wmsTransparent;

	@DatabaseField
	private boolean base = false;

	public Layer() {
		// ORMLite needs a no-arg constructor
	}

	public Layer(String remoteId, String type, String name, Event event) {
		super();
		this.remoteId = remoteId;
		this.type = type;
		this.name = name;
		this.event = event;
	}

	public Long getId() {
		return id;
	}

	public String getRemoteId() {
		return remoteId;
	}

	public void setRemoteId(String remoteId) {
		this.remoteId = remoteId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Event getEvent() {
		return event;
	}

	public void setEvent(Event event) {
		this.event = event;
	}

	public Long getDownloadId() {
		return downloadId;
	}

	public void setDownloadId(Long downloadId) {
		this.downloadId = downloadId;
	}

	public boolean isLoaded() {
		return loaded;
	}

	public void setLoaded(boolean loaded) {
		this.loaded = loaded;
	}

	public String getRelativePath() {
		return relativePath;
	}

	public void setRelativePath(String relativePath) {
		this.relativePath = relativePath;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public Long getFileSize() {
		return fileSize;
	}

	public void setFileSize(Long fileSize) {
		this.fileSize = fileSize;
	}

	public Collection<StaticFeature> getStaticFeatures() {
		return staticFeatures;
	}

	public void setUrl(String url){this.url = url;}

	public String getUrl(){return this.url;}

	public void setFormat(String format){this.format = format;}

	public String getFormat(){return this.format;}

	public String getWmsFormat() {
		return wmsFormat;
	}

	public void setWmsFormat(String wmsFormat) {
		this.wmsFormat = wmsFormat;
	}

	public String getWmsVersion() {
		return wmsVersion;
	}

	public void setWmsVersion(String wmsVersion) {
		this.wmsVersion = wmsVersion;
	}

	public String getWmsLayers() {
		return wmsLayers;
	}

	public void setWmsLayers(String wmsLayers) {
		this.wmsLayers = wmsLayers;
	}

	public String getWmsStyles() {
		return wmsStyles;
	}

	public void setWmsStyles(String wmsStyles) {
		this.wmsStyles = wmsStyles;
	}

	public String getWmsTransparent() {
		return wmsTransparent;
	}

	public void setWmsTransparent(String wmsTransparent) {
		this.wmsTransparent = wmsTransparent;
	}

	public boolean getBase() {
		return base;
	}

	public void setBase(boolean base) {
		this.base = base;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	@Override
	public int compareTo(Layer another) {
		return new CompareToBuilder().append(this.id, another.id).append(this.remoteId, another.remoteId).toComparison();
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
		Layer other = (Layer) obj;

		boolean eq = new EqualsBuilder().append(remoteId, other.remoteId).isEquals();
		return eq;
	}
}
