package mil.nga.giat.mage.sdk.http.converter;

import android.content.Context;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * Retrofit converter factory for locations
 *
 * @author newmanw
 *
 */
public final class UserConverterFactory extends Converter.Factory {

    private final Context context;

    public static UserConverterFactory create(Context context) {
        return new UserConverterFactory(context);
    }

    private UserConverterFactory(Context context) {
        this.context = context;
    }

    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
        return new UserResponseBodyConverter(context);
    }
}
