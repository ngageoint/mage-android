package mil.nga.giat.mage.form;

/**
 * This needs to match MageFormElement in attrs.xml.
 * 
 * @author wiedemanns
 * 
 */
public enum MagePropertyType {
	STRING, DATE, MULTILINE, MULTICHOICE, USER, LOCATION;

	public static MagePropertyType getPropertyType(int code) {
		switch (code) {
		case 0:
			return MagePropertyType.STRING;
		case 1:
			return MagePropertyType.DATE;
		case 2:
			return MagePropertyType.MULTILINE;
		case 3:
			return MagePropertyType.MULTICHOICE;
		case 4:
			return MagePropertyType.USER;
		case 5:
			return MagePropertyType.LOCATION;
		default:
			return MagePropertyType.STRING;
		}
	}
}
