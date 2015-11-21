package mil.nga.giat.mage.sdk.gson.deserializer;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import mil.nga.giat.mage.sdk.datastore.user.Permission;
import mil.nga.giat.mage.sdk.datastore.user.Permissions;
import mil.nga.giat.mage.sdk.datastore.user.Role;

/**
 * JSON to {@link Role}
 * 
 * @author wiedemanns
 * 
 */
public class RoleDeserializer implements JsonDeserializer<Collection<Role>> {

	private static final String LOG_NAME = RoleDeserializer.class.getName();

	/**
	 * Convenience method for returning a Gson object with a registered GSon
	 * TypeAdaptor i.e. custom deserializer.
	 * 
	 * @return A Gson object that can be used to convert Json into a {@link Role}.
	 */
	public static Gson getGsonBuilder() {
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(new TypeToken<Collection<Role>>(){}.getType(), new RoleDeserializer());
		return gsonBuilder.create();
	}

	@Override
	public Collection<Role> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		Collection<Role> roles = new ArrayList<>();

		JsonArray jsonRoles = json.getAsJsonArray();
		for (JsonElement element : jsonRoles) {
			JsonObject jsonRole = element.getAsJsonObject();

			String remoteId = jsonRole.get("id").getAsString();
			String name = jsonRole.get("name").getAsString();
			String description = jsonRole.get("description").getAsString();

			JsonArray jsonPermissions = jsonRole.get("permissions").getAsJsonArray();

			Collection<Permission> permissions = new ArrayList<>();
			for (int i = 0; i < jsonPermissions.size(); i++) {
				String jsonPermission = jsonPermissions.get(i).getAsString();
				if (jsonPermission != null) {
					jsonPermission = jsonPermission.toUpperCase(Locale.US);
					try {
						Permission permission = Permission.valueOf(jsonPermission);
						permissions.add(permission);
					} catch (IllegalArgumentException iae) {
						Log.e(LOG_NAME, "Could not find matching permission, " + jsonPermission + ", for user.");
					}
				}
			}

			Role role = new Role(remoteId, name, description, new Permissions(permissions));
			roles.add(role);
		}

		return roles;
	}
}
