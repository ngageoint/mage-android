package mil.nga.giat.mage.sdk.login;

import java.util.Date;

import mil.nga.giat.mage.sdk.datastore.user.User;

/**
 * Contains information from resulting authentication
 */
public class AuthorizationStatus {

	public enum Status {
		SUCCESSFUL_AUTHORIZATION,
		FAILED_AUTHORIZATION,
		FAILED_AUTHENTICATION,
		INVALID_SERVER
	}

	/**
	 * Request was successful or not
	 */
	private Status status;

	/**
	 * Contains information relevant to authorization,
	 *  such as a user's api token
	 */
	private User user;

	/**
	 * MAGE API Token used for api access
	 */
	private String token;

	/**
	 * MAGE API Token expiration
	 */
	private Date tokenExpiration;

	/**
	 * Message associated with authentication
	 */
	private String message;

	private AuthorizationStatus() {

	}

	public final Status getStatus() {
		return status;
	}

	public final User getUser() {
		return user;
	}

	public final String getToken() {
		return token;
	}

	public final Date getTokenExpiration() {
		return tokenExpiration;
	}

	public final String getMessage() {
		return message;
	}

	public static class Builder {
		private Status status;
		private User user;
		private String token;
		private Date tokenExpiration;
		private String message = null;

		public Builder(Status status) {
			this.status = status;
		}

		public Builder authorization(User user, String token) {
			this.user = user;
			this.token = token;
			return this;
		}

		public Builder tokenExpiration(Date tokenExpiration) {
			this.tokenExpiration = tokenExpiration;
			return this;
		}

		public Builder message(String message) {
			this.message = message;
			return this;
		}

		public AuthorizationStatus build() {
			AuthorizationStatus authentication = new AuthorizationStatus();
			authentication.status = this.status;
			authentication.user = this.user;
			authentication.token = this.token;
			authentication.tokenExpiration = this.tokenExpiration;
			authentication.message = this.message;

			return authentication;
		}
	}

}
