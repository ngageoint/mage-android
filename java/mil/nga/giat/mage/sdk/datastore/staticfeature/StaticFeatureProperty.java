package mil.nga.giat.mage.sdk.datastore.staticfeature;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;

import mil.nga.giat.mage.sdk.datastore.Property;

@DatabaseTable(tableName = "staticfeature_properties")
public class StaticFeatureProperty extends Property {

	@DatabaseField(foreign = true, uniqueCombo = true)
	private StaticFeature staticFeature;

	public StaticFeatureProperty() {
		// ORMLite needs a no-arg constructor
	}

	public StaticFeatureProperty(String pKey, String pValue) {
		super(pKey, pValue);
	}

	public StaticFeature getStaticFeature() {
		return staticFeature;
	}

	public void setStaticFeature(StaticFeature staticFeature) {
		this.staticFeature = staticFeature;
	}

	@Override
	public String getValue() {
		Serializable t = super.getValue();
		return (t == null)?null:t.toString();
	}
}
