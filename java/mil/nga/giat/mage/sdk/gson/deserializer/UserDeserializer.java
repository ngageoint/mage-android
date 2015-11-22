package mil.nga.giat.mage.sdk.gson.deserializer;

import android.content.Context;
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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import mil.nga.giat.mage.sdk.datastore.user.Phone;
import mil.nga.giat.mage.sdk.datastore.user.Role;
import mil.nga.giat.mage.sdk.datastore.user.RoleHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.exceptions.RoleException;

/**
 * JSON to {@link User}
 * 
 * @author newmanw
 * 
 */
public class UserDeserializer implements JsonDeserializer<Map.Entry<User, Collection<String>>> {

	private static final String LOG_NAME = UserDeserializer.class.getName();

	private Context mContext;

	public UserDeserializer(Context context) {
		this.mContext = context;
	}
	
	/**
	 * Convenience method for returning a Gson object with a registered GSon
	 * TypeAdaptor i.e. custom deserializer.
	 * 
	 * @return A Gson object that can be used to convert Json into a {@link User}.
	 */
	public static Gson getGsonBuilder(Context context) {
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(new TypeToken<Map.Entry<User, Collection<String>>>(){}.getType(), new UserDeserializer(context));
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
	public Map.Entry<User, Collection<String>> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		JsonObject jsonUser = json.getAsJsonObject();

		String remoteId = jsonUser.get("id").getAsString();
		
		String email = "";
		JsonElement emailElement = jsonUser.get("email");
		if (emailElement != null) {
		    email = emailElement.getAsString();
		}
		String displayName = jsonUser.get("displayName").getAsString();
		String username = jsonUser.get("username").getAsString();

        Role role = null;
        if (jsonUser.get("role") != null) {
            if (jsonUser.get("role").isJsonObject()) {
				JsonObject roleJSON = jsonUser.get("role").getAsJsonObject();
				if (roleJSON != null) {
					String roleId = roleJSON.get("id").getAsString();
					if (roleId != null) {
						try {
							// see if roles exists already
							role = RoleHelper.getInstance(mContext).read(roleId);
							// if it doesn't exist, then make it!
							if (role == null) {
								final Gson roleDeserializer = RoleDeserializer.getGsonBuilder();
								role = RoleHelper.getInstance(mContext).create(roleDeserializer.fromJson(roleJSON.toString(), Role.class));
								Log.i(LOG_NAME, "Created role with remote_id " + role.getRemoteId());
							}
						} catch (RoleException e) {
							Log.e(LOG_NAME, "Could not find matching role for user.");
						}
					} else {
						Log.e(LOG_NAME, "User has role with no id!");
					}
				}
			}
        } else if(jsonUser.get("roleId") != null) {
			if (jsonUser.get("roleId").isJsonPrimitive()) {
				String roleId = jsonUser.get("roleId").getAsString();
				if (roleId != null) {
					try {
						// go get role
						role = RoleHelper.getInstance(mContext).read(roleId);
					} catch (RoleException e) {
						Log.e(LOG_NAME, "Could not find matching role for user.");
					}
				} else {
					Log.e(LOG_NAME, "User has role with no id!");
				}
			}
        } else {
			Log.e(LOG_NAME, "User has no role!");
		}

        if (role == null) {
            throw new JsonParseException("Unable to find or make role for user!");
        }
		
		Collection<Phone> phones = new ArrayList<Phone>();
		String primaryPhone = null;
		if (jsonUser.has("phones")) {
			JsonArray phoneArray = jsonUser.get("phones").getAsJsonArray();
			for (JsonElement e : phoneArray) {
				Phone phone = new Phone();
				phone.setNumber(e.getAsJsonObject().get("number").getAsString());
				phones.add(phone);
				if (primaryPhone == null) {
					primaryPhone = phone.getNumber();
				}
			}
		}

		String avatarUrl = null;
		if (jsonUser.has("avatarUrl")) {
			avatarUrl = jsonUser.get("avatarUrl").getAsString();
		}
		
		String iconUrl = null;
		if (jsonUser.has("iconUrl")) {
			iconUrl = jsonUser.get("iconUrl").getAsString();
		}

		Collection<String> recentEventIds = new ArrayList<>();
        if (jsonUser.get("recentEventIds") != null && jsonUser.get("recentEventIds").isJsonArray()) {
			for (JsonElement element : jsonUser.get("recentEventIds").getAsJsonArray()) {
				recentEventIds.add(element.getAsString());
			}
        } else {
            Log.w(LOG_NAME, "User has no recent events!");
        }

		User user = new User(remoteId, email, displayName, username, role, null, primaryPhone, avatarUrl, iconUrl);
		return new AbstractMap.SimpleEntry<User, Collection<String>>(user, recentEventIds);
	}
}
