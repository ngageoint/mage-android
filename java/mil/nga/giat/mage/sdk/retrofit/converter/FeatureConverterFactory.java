package mil.nga.giat.mage.sdk.retrofit.converter;

import com.squareup.okhttp.ResponseBody;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

import mil.nga.giat.mage.sdk.datastore.layer.Layer;
import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeature;
import retrofit.Converter;

/**
 * Retrofit converter factory for locations
 *
 * @author newmanw
 *
 */
public final class FeatureConverterFactory extends Converter.Factory {

    private Layer layer;

    public static FeatureConverterFactory create(Layer layer) {
        return new FeatureConverterFactory(layer);
    }

    private FeatureConverterFactory(Layer layer) {
        this.layer = layer;
    }

    @Override
    public Converter<ResponseBody, List<StaticFeature>> fromResponseBody(Type type, Annotation[] annotations) {
        return new FeatureResponseBodyConverter(layer);
    }
}
