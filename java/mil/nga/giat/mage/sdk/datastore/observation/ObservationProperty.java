package mil.nga.giat.mage.sdk.datastore.observation;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;

import mil.nga.giat.mage.sdk.datastore.Property;

@DatabaseTable(tableName = "observation_properties")
public class ObservationProperty extends Property {

	@DatabaseField(foreign = true, uniqueCombo = true)
	private ObservationForm observationForm;

	public ObservationProperty() {
		// ORMLite needs a no-arg constructor
	}

	public ObservationProperty(String pKey, Serializable pValue) {
		super(pKey, pValue);
	}

	public ObservationForm getObservation() {
		return observationForm;
	}

	public void setObservationForm(ObservationForm observationForm) {
		this.observationForm = observationForm;
	}

}
