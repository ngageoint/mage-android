package mil.nga.giat.mage.form;

import java.io.Serializable;

public interface MageControl {

	public void setPropertyKey(String propertyKey);

	public String getPropertyKey();

	public Serializable getPropertyValue();

	public void setPropertyType(MagePropertyType propertyType);

	public MagePropertyType getPropertyType();

	public Boolean isRequired();

	public void setRequired(Boolean required);

}
