package mil.nga.giat.mage.sdk.gson.deserializer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import mil.nga.giat.mage.sdk.datastore.user.Phone;
import mil.nga.giat.mage.sdk.datastore.user.Role;
import mil.nga.giat.mage.sdk.datastore.user.RoleHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.exceptions.RoleException;
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

/**
 * JSON to {@link User}
 * 
 * @author wiedemannse
 * 
 */
public class UserDeserializer implements JsonDeserializer<User> {

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
		gsonBuilder.registerTypeAdapter(User.class, new UserDeserializer(context));
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
	public User deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

		JsonObject feature = json.getAsJsonObject();

		String remoteId = feature.get("id").getAsString();
		
		String email = "";
		JsonElement emailElement = feature.get("email");
		if (emailElement != null) {
		    email = emailElement.getAsString();
		}
		String firstname = feature.get("firstname").getAsString();
		String lastname = feature.get("lastname").getAsString();
		String username = feature.get("username").getAsString();

		Role role = null;
		if (feature.get("role") != null) {

            String roleId = null;
            JsonObject roleJSON = null;
            if(feature.get("role").isJsonObject()) {
                roleJSON = feature.get("role").getAsJsonObject();
                if (roleJSON != null) {
                    roleId = roleJSON.get("id").getAsString();
                }
            } else if(feature.get("role").isJsonPrimitive()) {
                roleId = feature.get("role").getAsString();
            }

            if(roleId != null) {
				try {
					// see if roles exists already
					role = RoleHelper.getInstance(mContext).read(roleId);
					// if it doesn't exist, then make it!
					if (role == null && roleJSON != null) {
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
		} else {
			Log.e(LOG_NAME, "User has no role!");
		}

        if(role == null) {
            throw new JsonParseException("Unable to find or make role for user!");
        }
		
		Collection<Phone> phones = new ArrayList<Phone>();
		String primaryPhone = null;
		if (feature.has("phones")) {
			JsonArray phoneArray = feature.get("phones").getAsJsonArray();
			for (Iterator<JsonElement> i = phoneArray.iterator(); i.hasNext();) {
				JsonElement e = i.next();
				Phone phone = new Phone();
				phone.setNumber(e.getAsJsonObject().get("number").getAsString());
				phones.add(phone);
				if (primaryPhone == null) {
					primaryPhone = phone.getNumber();
				}
			}
		}
		String avatarUrl = null;
		if (feature.has("avatarUrl")) {
			avatarUrl = feature.get("avatarUrl").getAsString();
		}
		
		String iconUrl = null;
		if (feature.has("iconUrl")) {
			iconUrl = feature.get("iconUrl").getAsString();
		}

		User user = new User(remoteId, email, firstname, lastname, username, role, primaryPhone, avatarUrl, iconUrl);
		return user;
	}
}
