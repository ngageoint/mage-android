package mil.nga.giat.mage.sdk.http.converter;

import android.content.Context;

import com.squareup.okhttp.ResponseBody;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import mil.nga.giat.mage.sdk.datastore.user.User;
import retrofit.Converter;

/**
 * Retrofit converter factory for locations
 *
 * @author newmanw
 *
 */
public final class UserConverterFactory extends Converter.Factory {

    private Context context;

    public static UserConverterFactory create(Context context) {
        return new UserConverterFactory(context);
    }

    private UserConverterFactory(Context context) {
        this.context = context;
    }

    @Override
    public Converter<ResponseBody, User> fromResponseBody(Type type, Annotation[] annotations) {
        return new UserResponseBodyConverter(context);
    }
}
