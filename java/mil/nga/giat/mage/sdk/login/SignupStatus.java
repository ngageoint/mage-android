package mil.nga.giat.mage.sdk.login;

import com.google.gson.JsonObject;

/**
 * Contains information from resulting authentication
 */
public class SignupStatus {

	public enum Status {
		SUCCESSFUL_SIGNUP,
		FAILED_SIGNUP
	}

	/**
	 * Request was successful or not
	 */
	private Status status = Status.FAILED_SIGNUP;

	/**
	 * Contains information relevant to authorization,
	 *  such as a user's api token
	 */
	private JsonObject user = new JsonObject();

	/**
	 * Message associated with authentication
	 */
	private String message;

	private SignupStatus() {

	}

	public final Status getStatus() {
		return status;
	}

	public final JsonObject getUser() {
		return user;
	}

	public final String getMessage() {
		return message;
	}

	public static class Builder {
		private Status status;
		private String message = null;
		private JsonObject user = null;

		public Builder(Status status) {
			this.status = status;
		}

		public Builder message(String message) {
			this.message = message;
			return this;
		}

		public Builder user(JsonObject user) {
			this.user = user;
			return this;
		}

		public SignupStatus build() {
			SignupStatus authentication = new SignupStatus();
			authentication.status = this.status;
			authentication.message = this.message;
			authentication.user = this.user;

			return authentication;
		}
	}

}
