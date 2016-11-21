package mil.nga.giat.mage.sdk.datastore.staticfeature;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;
import com.vividsolutions.jts.geom.Geometry;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import mil.nga.giat.mage.sdk.datastore.layer.Layer;

@DatabaseTable(tableName = "staticfeatures")
public class StaticFeature implements Comparable<StaticFeature> {

	public static final String STATIC_FEATURE_ID = "id";
	public static final String STATIC_FEATURE_REMOTE_ID = "remote_id";
	public static final String STATIC_FEATURE_LAYER_ID = "layer_id";

	@DatabaseField(generatedId = true)
	private Long id;

	@DatabaseField(unique = true, columnName = STATIC_FEATURE_REMOTE_ID)
	private String remoteId;

	@DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true, columnName = STATIC_FEATURE_LAYER_ID)
	private Layer layer;

	@DatabaseField(canBeNull = false, dataType = DataType.SERIALIZABLE)
	private Geometry geometry;

	@ForeignCollectionField(eager = true)
	private Collection<StaticFeatureProperty> properties = new ArrayList<StaticFeatureProperty>();
	
	@DatabaseField(columnName="local_path")
	private String localPath;
	
	public StaticFeature() {
		// ORMLite needs a no-arg constructor
	}

	public StaticFeature(Geometry geometry, Layer layer) {
		this(null, geometry, layer);
	}

	public StaticFeature(String remoteId, Geometry geometry, Layer layer) {
		super();
		this.remoteId = remoteId;
		this.geometry = geometry;
		this.layer = layer;
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

	public Layer getLayer() {
		return layer;
	}

	public void setLayer(Layer layer) {
		this.layer = layer;
	}

	public Geometry getGeometry() {
		return geometry;
	}

	public void setGeometry(Geometry geometry) {
		this.geometry = geometry;
	}

	public Collection<StaticFeatureProperty> getProperties() {
		return properties;
	}

	public void setProperties(Collection<StaticFeatureProperty> properties) {
		this.properties = properties;
	}
	
	public String getLocalPath() {
		return localPath;
	}

	public void setLocalPath(String localPath) {
		this.localPath = localPath;
	}
	
	   /**
     * A convenience method used for returning an Observation's properties in a
     * more useful data-structure.
     * 
     * @return
     */
    public final Map<String, StaticFeatureProperty> getPropertiesMap() {
        Map<String, StaticFeatureProperty> propertiesMap = new HashMap<String, StaticFeatureProperty>();
        for (StaticFeatureProperty property : properties) {
            propertiesMap.put(property.getKey(), property);
        }

        return propertiesMap;
    }

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	@Override
	public int compareTo(StaticFeature another) {
		return new CompareToBuilder().append(this.id, another.id).append(this.remoteId, another.remoteId).toComparison();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		StaticFeature other = (StaticFeature) obj;
		return new EqualsBuilder().appendSuper(super.equals(obj)).append(id, other.id).append(remoteId, other.remoteId).isEquals();
	}

}