package mil.nga.giat.mage.sdk.http.converter;

import android.content.Context;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

import mil.nga.giat.mage.sdk.datastore.user.User;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

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
    public Converter<ResponseBody, List<User>> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
        return new UsersResponseBodyConverter(context);
    }
}
