package mil.nga.giat.mage.sdk.datastore.observation;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@DatabaseTable(tableName = "observation_form")
public class ObservationForm {
	public static final String COLUMN_NAME_FORM_ID = "form_id";

	@DatabaseField(generatedId = true)
	private Long _id;

	@DatabaseField(columnName = COLUMN_NAME_FORM_ID)
	private Long formId;

	@DatabaseField(foreign = true)
	private Observation observation;

	@ForeignCollectionField(eager = true)
	private Collection<ObservationProperty> properties = new ArrayList<>();

	public ObservationForm() {
		// ORMLite needs a no-arg constructor
	}

	public Long getId() {
		return _id;
	}

	public void setId(Long _id) {
		this._id = _id;
	}

	public Long getFormId() {
		return formId;
	}

	public void setFormId(Long formId) {
		this.formId = formId;
	}

	public void setObservation(Observation observation) {
		this.observation = observation;
	}

	public Collection<ObservationProperty> getProperties() {
		return properties;
	}

	public void setProperties(Collection<ObservationProperty> properties) {
		this.properties = properties;
	}

	public void addProperties(Collection<ObservationProperty> properties) {

		Map<String, ObservationProperty> newPropertiesMap = new HashMap<String, ObservationProperty>();
		for (ObservationProperty property : properties) {
			property.setObservationForm(this);
			newPropertiesMap.put(property.getKey(), property);
		}

		Map<String, ObservationProperty> oldPropertiesMap = getPropertiesMap();

		oldPropertiesMap.putAll(newPropertiesMap);
		this.properties = oldPropertiesMap.values();
	}

	/**
	 * A convenience method used for returning an Observation's properties in a
	 * more useful data-structure.
	 *
	 * @return
	 */
	public final Map<String, ObservationProperty> getPropertiesMap() {
		Map<String, ObservationProperty> propertiesMap = new HashMap<>();
		for (ObservationProperty property : properties) {
			propertiesMap.put(property.getKey(), property);
		}

		return propertiesMap;
	}

}
