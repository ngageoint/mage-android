package mil.nga.giat.mage.sdk.gson.deserializer;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import mil.nga.giat.mage.sdk.datastore.user.User;

/**
 * JSON to {@link User}
 * 
 * @author newmanw
 * 
 */
public class UsersDeserializer implements JsonDeserializer<Map<User, Collection<String>>> {

	private static final String LOG_NAME = UsersDeserializer.class.getName();

	private Gson userDeserializer;
	
	public UsersDeserializer(Context context) {
		userDeserializer = UserDeserializer.getGsonBuilder(context);
	}
	
	/**
	 * Convenience method for returning a Gson object with a registered GSon
	 * TypeAdaptor i.e. custom deserializer.
	 * 
	 * @return A Gson object that can be used to convert Json into a {@link User}.
	 */
	public static Gson getGsonBuilder(Context context) {
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(new TypeToken<Map<User, Collection<String>>>(){}.getType(), new UsersDeserializer(context));
		return gsonBuilder.create();
	}

	/**
	 * This is used for both the /api/login response and the /api/users response
	 *
	 * @param json
	 * @param typeOfT
	 * @param context
	 * @return
	 * @throws JsonParseException
	 */
	@Override
	public Map<User, Collection<String>> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		Map<User, Collection<String>> users = new HashMap<>();

		for (JsonElement element : json.getAsJsonArray()) {
			Map.Entry<User, Collection<String>> user = userDeserializer.fromJson(element, new TypeToken<Map.Entry<User, Collection<String>>>() {}.getType());
			users.put(user.getKey(), user.getValue());
		}

		return users;
	}
}
