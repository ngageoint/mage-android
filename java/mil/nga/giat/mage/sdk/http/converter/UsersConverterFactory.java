package mil.nga.giat.mage.sdk.http.converter;

import android.content.Context;

import com.squareup.okhttp.ResponseBody;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

import mil.nga.giat.mage.sdk.datastore.user.User;
import retrofit.Converter;

/**
 * Retrofit converter factory for locations
 *
 * @author newmanw
 *
 */
public final class UsersConverterFactory extends Converter.Factory {

    private Context context;

    public static UsersConverterFactory create(Context context) {
        return new UsersConverterFactory(context);
    }

    private UsersConverterFactory(Context context) {
        this.context = context;
    }

    @Override
    public Converter<ResponseBody, List<User>> fromResponseBody(Type type, Annotation[] annotations) {
        return new UsersResponseBodyConverter(context);
    }
}
