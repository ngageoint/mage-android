package mil.nga.giat.mage.sdk.datastore.user;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

public class Permissions implements Serializable {

	private static final long serialVersionUID = -1912604919150929355L;

	private Collection<Permission> permissions = new ArrayList<Permission>();

	public Permissions() {

	}

	public Permissions(Collection<Permission> permissions) {
		super();
		this.permissions = permissions;
	}

	public Collection<Permission> getPermissions() {
		return permissions;
	}

	public void setPermissions(Collection<Permission> permissions) {
		this.permissions = permissions;
	}

}
