package mil.nga.giat.mage.sdk.http.converter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

import mil.nga.giat.mage.sdk.datastore.layer.Layer;
import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeature;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * Retrofit converter factory for features
 *
 * @author newmanw
 *
 */
public final class FeatureConverterFactory extends Converter.Factory {

    private final Layer layer;

    public static FeatureConverterFactory create(Layer layer) {
        return new FeatureConverterFactory(layer);
    }

    private FeatureConverterFactory(Layer layer) {
        this.layer = layer;
    }

    @Override
    public Converter<ResponseBody, List<StaticFeature>> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
        return new FeatureResponseBodyConverter(layer);
    }
}
