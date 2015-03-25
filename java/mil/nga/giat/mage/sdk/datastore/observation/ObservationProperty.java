package mil.nga.giat.mage.sdk.datastore.observation;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;

import mil.nga.giat.mage.sdk.datastore.Property;

@DatabaseTable(tableName = "observation_properties")
public class ObservationProperty extends Property {

	@DatabaseField(foreign = true, uniqueCombo = true)
	private Observation observation;

	public ObservationProperty() {
		// ORMLite needs a no-arg constructor
	}

	public ObservationProperty(String pKey, Serializable pValue) {
		super(pKey, pValue);
	}

	public Observation getObservation() {
		return observation;
	}

	public void setObservation(Observation observation) {
		this.observation = observation;
	}

}
