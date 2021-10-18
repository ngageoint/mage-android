package mil.nga.giat.mage.sdk.gson.deserializer;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

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
public class RoleDeserializer implements JsonDeserializer<Role> {

	private static final String LOG_NAME = RoleDeserializer.class.getName();

	/**
	 * Convenience method for returning a Gson object with a registered GSon
	 * TypeAdaptor i.e. custom deserializer.
	 * 
	 * @return A Gson object that can be used to convert Json into a {@link Role}.
	 */
	public static Gson getGsonBuilder() {
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(Role.class, new RoleDeserializer());
		return gsonBuilder.create();
	}

	@Override
	public Role deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		JsonObject jsonRole = json.getAsJsonObject();

		String remoteId = jsonRole.get("id").getAsString();
		String name = jsonRole.get("name").getAsString();
		String description = jsonRole.get("description").getAsString();


		Collection<Permission> permissions = new ArrayList<>();
		for (JsonElement element : jsonRole.get("permissions").getAsJsonArray()) {
			String jsonPermission = element.getAsString();
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

		return new Role(remoteId, name, description, new Permissions(permissions));
	}
}
