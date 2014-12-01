package mil.nga.giat.mage.sdk.datastore.staticfeature;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "staticfeature_properties")
public class StaticFeatureProperty {
	
	@DatabaseField(generatedId = true)
	private Long pk_id;

	@DatabaseField(canBeNull = false, uniqueCombo = true)
	private String key;

	@DatabaseField
	private String value;

	@DatabaseField(foreign = true, uniqueCombo = true)
	private StaticFeature staticFeature;

	public StaticFeatureProperty() {
		// ORMLite needs a no-arg constructor
	}

	public StaticFeatureProperty(String pKey, String pValue) {
		this.key = pKey;
		this.value = pValue;
	}

	public Long getPk_id() {
		return pk_id;
	}

	public void setPk_id(Long pk_id) {
		this.pk_id = pk_id;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public StaticFeature getStaticFeature() {
		return staticFeature;
	}

	public void setStaticFeature(StaticFeature staticFeature) {
		this.staticFeature = staticFeature;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

}
