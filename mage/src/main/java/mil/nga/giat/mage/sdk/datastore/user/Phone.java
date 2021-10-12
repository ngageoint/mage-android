package mil.nga.giat.mage.sdk.datastore.user;

import java.io.Serializable;

public class Phone implements Serializable {

	private static final long serialVersionUID = -2050927399100074414L;

	private String number;

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}
}
