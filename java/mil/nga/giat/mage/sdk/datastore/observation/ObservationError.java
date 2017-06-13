package mil.nga.giat.mage.sdk.datastore.observation;

public class ObservationError {

	private Integer statusCode;
	private String message;
	private String description;

	public ObservationError() {
		// ORMLite needs a no-arg constructor
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Integer getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(Integer statusCode) {
		this.statusCode = statusCode;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}
