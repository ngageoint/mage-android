package mil.nga.giat.mage.form;

public interface MageControl {

	public void setPropertyKey(String propertyKey);

	public String getPropertyKey();

	public String getPropertyValue();

	public void setPropertyType(MagePropertyType propertyType);

	public MagePropertyType getPropertyType();

	public Boolean isRequired();

	public void setRequired(Boolean required);

}
