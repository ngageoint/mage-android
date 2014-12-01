package mil.nga.giat.mage.sdk.datastore.location;

import java.io.Serializable;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "location_properties")
public class LocationProperty {

	@DatabaseField(generatedId = true)
	private Long _id;

	@DatabaseField(canBeNull = false, uniqueCombo = true)
	private String key;

	@DatabaseField(canBeNull = false, dataType=DataType.SERIALIZABLE)
	private Serializable value;
	
	@DatabaseField(foreign = true, uniqueCombo = true)
	private Location location;

	public LocationProperty() {
		// ORMLite needs a no-arg constructor
	}

	public LocationProperty(String pKey, Serializable pValue) {
		this.key = pKey;
		this.value = pValue;
	}

	public Long getId() {
		return _id;
	}

	public void setId(Long _id) {
		this._id = _id;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Serializable getValue() {
		return value;
	}

	public void setValue(Serializable value) {
		this.value = value;
	}

	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

}
