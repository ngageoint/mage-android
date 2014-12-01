package mil.nga.giat.mage.sdk.gson.deserializer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import mil.nga.giat.mage.sdk.datastore.user.Permission;
import mil.nga.giat.mage.sdk.datastore.user.Permissions;
import mil.nga.giat.mage.sdk.datastore.user.Role;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

/**
 * JSON to {@link Role}
 * 
 * @author wiedemannse
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

		JsonObject feature = json.getAsJsonObject();

		String remoteId = feature.get("_id").getAsString();
		String name = feature.get("name").getAsString();
		String description = feature.get("description").getAsString();
		
		JsonArray jsonPermissions = feature.get("permissions").getAsJsonArray();
		
		Collection<Permission> permissions = new ArrayList<Permission>();
		for (int i = 0; i < jsonPermissions.size(); i++) {
			String jsonPermission = jsonPermissions.get(i).getAsString();
			if (jsonPermission != null) {
				jsonPermission = jsonPermission.toUpperCase(Locale.US);
				try {
					Permission permission = Permission.valueOf(jsonPermission);
					permissions.add(permission);
				} catch (IllegalArgumentException iae) {
					Log.e(LOG_NAME, "Could not find matching permission for user.");
				}
			}
		}
		
		Role role = new Role(remoteId, name, description, new Permissions(permissions));
		return role;
	}
}
