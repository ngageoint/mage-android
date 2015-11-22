package mil.nga.giat.mage.sdk.gson.deserializer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;

import mil.nga.giat.mage.sdk.datastore.user.Role;

/**
 * JSON to {@link Role}
 * 
 * @author newmanw
 * 
 */
public class RolesDeserializer implements JsonDeserializer<Collection<Role>> {

	private static final String LOG_NAME = RolesDeserializer.class.getName();

	private Gson roleDeserializer;

	public RolesDeserializer() {
		roleDeserializer = RoleDeserializer.getGsonBuilder();
	}

	/**
	 * Convenience method for returning a Gson object with a registered GSon
	 * TypeAdaptor i.e. custom deserializer.
	 * 
	 * @return A Gson object that can be used to convert Json into a {@link Role}.
	 */
	public static Gson getGsonBuilder() {
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(new TypeToken<Collection<Role>>(){}.getType(), new RolesDeserializer());
		return gsonBuilder.create();
	}

	@Override
	public Collection<Role> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		Collection<Role> roles = new ArrayList<>();

		JsonArray jsonRoles = json.getAsJsonArray();
		for (JsonElement element : jsonRoles) {
			Role role = roleDeserializer.fromJson(element, Role.class);
			roles.add(role);
		}

		return roles;
	}
}
