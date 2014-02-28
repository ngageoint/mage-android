package mil.nga.giat.mage.sdk.login;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains information from resulting login or sign up
 * 
 * @author wiedemannse
 *
 */
public class AccountStatus {

	/**
	 * Request was successful or not
	 */
	private Boolean status = Boolean.FALSE;

	/**
	 * If status was false. Then this list can correspond to which argument(s)
	 * of {@link AbstractAccountTask#execute(String...)} that were problematic
	 * in the request.
	 */
	private List<Integer> errorIndices = new ArrayList<Integer>();

	/**
	 * Information about the problems if errorIndices is present
	 */
	private List<String> errorMessages = new ArrayList<String>();

	/**
	 * If status was true, contains information relevant to the
	 * {@link AbstractAccountTask}, such as a user's token
	 */
	private List<String> accountInformation = new ArrayList<String>();

	public AccountStatus(Boolean status) {
		super();
		this.status = status;
	}

	public AccountStatus(Boolean status, List<Integer> errorIndices, List<String> errorMessages) {
		super();
		this.status = status;
		this.errorIndices = errorIndices;
		this.errorMessages = errorMessages;
	}

	public AccountStatus(Boolean status, List<Integer> errorIndices, List<String> errorMessages, List<String> accountInformation) {
		super();
		this.status = status;
		this.errorIndices = errorIndices;
		this.errorMessages = errorMessages;
		this.accountInformation = accountInformation;
	}

	public final Boolean getStatus() {
		return status;
	}

	public final List<Integer> getErrorIndices() {
		return errorIndices;
	}

	public final List<String> getErrorMessages() {
		return errorMessages;
	}

	public final List<String> getAccountInformation() {
		return accountInformation;
	}
}
