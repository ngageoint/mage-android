package mil.nga.giat.mage.sdk.http.converter;

import android.content.Context;

import com.squareup.okhttp.ResponseBody;

import java.io.IOException;

import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.jackson.deserializer.UserDeserializer;
import retrofit.Converter;

/**
 * Retrofit location response body converter
 *
 * Handles Jackson deserialization for locations
 *
 * @author newmanw
 *
 */
public class UserResponseBodyConverter implements Converter<ResponseBody, User> {

    private Context context;

    public UserResponseBodyConverter(Context context) {
        this.context = context;
    }

    @Override
    public User convert(ResponseBody body) throws IOException {
        UserDeserializer deserializer = new UserDeserializer(context);
        return deserializer.parseUser(body.byteStream());
    }
}
