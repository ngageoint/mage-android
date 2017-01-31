package mil.nga.giat.mage.sdk.jackson.deserializer;

import android.content.Context;
import android.util.Log;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import mil.nga.giat.mage.sdk.datastore.user.Permission;
import mil.nga.giat.mage.sdk.datastore.user.Permissions;
import mil.nga.giat.mage.sdk.datastore.user.Phone;
import mil.nga.giat.mage.sdk.datastore.user.Role;
import mil.nga.giat.mage.sdk.datastore.user.RoleHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.exceptions.RoleException;

public class UserDeserializer extends Deserializer {

    private static final String LOG_NAME = UserDeserializer.class.getName();

	private RoleHelper roleHelper = null;
	private Map<String, Role> roles = new HashMap<>();

	public UserDeserializer(Context context) {
		this.roleHelper = RoleHelper.getInstance(context);
	}

	public List<User> parseUsers(InputStream is) throws IOException {
		getRoles();

		JsonParser parser = factory.createParser(is);

		List<User> users = new ArrayList<>();
		users.addAll(parseUsers(parser));
		parser.close();

		return users;
	}

	public User parseUser(InputStream is) throws IOException {
		JsonParser parser = factory.createParser(is);
		parser.nextToken();
		return parseUser(parser);
	}

	public User parseUser(String userJson) throws IOException {
		JsonParser parser = factory.createParser(userJson);
		parser.nextToken();
		return parseUser(parser);
	}

	private Collection<User> parseUsers(JsonParser parser) throws IOException {
		Collection<User> users = new ArrayList<>();
		parser.nextToken();
		while (parser.nextToken() != JsonToken.END_ARRAY) {
			users.add(parseUser(parser));
		}
		return users;
	}

	private User parseUser(JsonParser parser) throws IOException {
		User user = new User();

		if (parser.getCurrentToken() != JsonToken.START_OBJECT) {
			return user;
		}

		while (parser.nextToken() != JsonToken.END_OBJECT) {
			String name = parser.getCurrentName();
			parser.nextToken();
			if ("id".equals(name)) {
				user.setRemoteId(parser.getText());
			} else if ("email".equals(name)) {
				user.setEmail(parser.getText());
			} else if ("displayName".equals(name)) {
				user.setDisplayName(parser.getText());
			} else if ("username".equals(name)) {
				user.setUsername(parser.getText());
			} else if ("role".equals(name)) {
				user.setRole(parseRole(parser));
			} else if ("roleId".equals(name)) {
				user.setRole(parseRoleId(parser));
			} else if ("phones".equals(name)) {
				user.setPrimaryPhone(parsePrimaryPhone(parser));
			} else if ("avatarUrl".equals(name)) {
				user.setAvatarUrl(parser.getText());
			} else if ("iconUrl".equals(name)) {
				user.setIconUrl(parser.getText());
			}  else if ("recentEventIds".equals(name)) {
				user.setRecentEventId(parseRecentEventId(parser));
			} else {
				parser.skipChildren();
			}
		}

		if (user.getRole() == null) {
			throw new JsonParseException("Unable to find or make role for user!", JsonLocation.NA);
		}

		return user;
	}

	private Role parseRole(JsonParser parser) throws IOException {
		if (parser.getCurrentToken() != JsonToken.START_OBJECT) return null;

		Role role = new Role();
		while (parser.nextToken() != JsonToken.END_OBJECT) {
			String name = parser.getCurrentName();
			parser.nextToken();
			if ("id".equals(name)) {
				role.setRemoteId(parser.getText());
			} else if ("name".equals(name)) {
				role.setName(parser.getText());
			} else if ("description".equals(name)) {
				role.setDescription(parser.getText());
			} else if ("permissions".equals(name)) {
				role.setPermissions(parsePermissions(parser));
			} else {
				parser.skipChildren();
			}
		}

		role = roles.containsKey(role.getRemoteId()) ? roles.get(role.getRemoteId()) : roleHelper.createOrUpdate(role);
		return role;
	}

	private Permissions parsePermissions(JsonParser parser) throws IOException {
		if (parser.getCurrentToken() != JsonToken.START_ARRAY) return null;

		Collection<Permission> permissions = new ArrayList<>();
		while (parser.nextToken() != JsonToken.END_ARRAY) {
			parser.nextToken();
			String permission = parser.getText().toUpperCase(Locale.US);
			permissions.add(Permission.valueOf(permission));
		}

		return new Permissions(permissions);
	}

	private Role parseRoleId(JsonParser parser) throws IOException {
		String roleId = parser.getText();

		Role role = null;
		try {
			role = roleHelper.read(roleId);
		} catch (RoleException e) {
			Log.e(LOG_NAME, "Could not find matching role for user.");
		}

		return role;
	}

	private String parsePrimaryPhone(JsonParser parser) throws IOException {
		if (parser.getCurrentToken() != JsonToken.START_ARRAY) return null;

		String primaryPhone = null;
		while (parser.nextToken() != JsonToken.END_ARRAY) {
			Phone phone = parsePhone(parser);
			if (primaryPhone == null) {
				primaryPhone = phone.getNumber();
			}
		}

		return primaryPhone;
	}

	private Phone parsePhone(JsonParser parser) throws IOException {
		Phone phone = new Phone();

		if (parser.getCurrentToken() != JsonToken.START_OBJECT) return phone;

		while (parser.nextToken() != JsonToken.END_OBJECT) {
			String name = parser.getCurrentName();
			parser.nextToken();
			if ("number".equals(name)) {
				phone.setNumber(parser.getText());
			} else {
				parser.skipChildren();
			}
		}

		return phone;
	}

	private String parseRecentEventId(JsonParser parser) throws IOException {
		if (parser.getCurrentToken() != JsonToken.START_ARRAY) return null;

		String recentEventId = null;
		while (parser.nextToken() != JsonToken.END_ARRAY) {
			String eventId = parser.getText();
			if (recentEventId == null) {
				recentEventId = eventId;
			}
		}

		return recentEventId;
	}

	private void getRoles() {
		try {
			for (Role role : roleHelper.readAll()) {
				roles.put(role.getRemoteId(), role);
			}
		} catch (RoleException e) {
			e.printStackTrace();
		}
	}
}