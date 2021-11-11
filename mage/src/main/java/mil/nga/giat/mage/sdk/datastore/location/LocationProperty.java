package mil.nga.giat.mage.sdk.datastore.location;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;

import mil.nga.giat.mage.sdk.datastore.Property;

@DatabaseTable(tableName = "location_properties")
public class LocationProperty extends Property {

	@DatabaseField(foreign = true, uniqueCombo = true)
	private Location location;

	public LocationProperty() {
		// ORMLite needs a no-arg constructor
	}

	public LocationProperty(String pKey, Serializable pValue) {
		super(pKey, pValue);
	}

	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

}
