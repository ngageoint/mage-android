package mil.nga.giat.mage.sdk.login;

/**
 * Contains information from resulting authentication
 */
public class AuthenticationStatus {

	public enum Status {
		SUCCESSFUL_AUTHENTICATION,
		DISCONNECTED_AUTHENTICATION,
		ACCOUNT_CREATED,
		FAILED_AUTHENTICATION,
	}

	/**
	 * Request was successful or not
	 */
	private Status status = Status.FAILED_AUTHENTICATION;

	/**
	 * JSON Web Token used for authorization
	 */
	private String token;

	/**
	 * Message associated with authentication
	 */
	private String message;

	private AuthenticationStatus() {

	}

	public final Status getStatus() {
		return status;
	}

	public final String getToken() {
		return token;
	}

	public final String getMessage() {
		return message;
	}

	public static class Builder {
		private final Status status;
		private String token = "";
		private String message = null;

		public Builder(Status status) {
			this.status = status;
		}

		public Builder token(String token) {
			this.token = token;
			return this;
		}

		public Builder message(String message) {
			this.message = message;
			return this;
		}

		public AuthenticationStatus build() {
			AuthenticationStatus authentication = new AuthenticationStatus();
			authentication.status = this.status;
			authentication.token = this.token;
			authentication.message = this.message;

			return authentication;
		}
	}

}
