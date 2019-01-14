package mil.nga.giat.mage.sdk.datastore;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Common stuff across observations, locations, static features, etc.
 *
 * @author wiedemanns
 */
public abstract class Property {

	@DatabaseField(generatedId = true)
	private Long _id;

	@DatabaseField(canBeNull = false, uniqueCombo = true)
	private String key;

	@DatabaseField(canBeNull = false, dataType = DataType.SERIALIZABLE)
	private Serializable value;

	public Property() {
	}

	public Property(String pKey, Serializable pValue) {
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

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}

		if (!(other instanceof Property)) {
			return false;
		}

		Property property = (Property) other;

		if (!key.equals(property.key)) {
			return false;
		}

		if (!value.getClass().equals(property.value.getClass())) {
			return false;
		}

		if (value instanceof ArrayList) {
            ArrayList<?> lhs = (ArrayList<?>) value;
            ArrayList<?> rhs = (ArrayList<?>) property.value;
            return lhs.size() == rhs.size() && lhs.containsAll(rhs);
		} else {
			return value.equals(property.value);
		}
	}

}
