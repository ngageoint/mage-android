package mil.nga.giat.mage.sdk.http.converter;

import java.io.IOException;
import java.util.List;

import mil.nga.giat.mage.sdk.datastore.layer.Layer;
import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeature;
import mil.nga.giat.mage.sdk.jackson.deserializer.StaticFeatureDeserializer;
import okhttp3.ResponseBody;
import retrofit2.Converter;

/**
 * Retrofit feature response body converter
 *
 * Handles Jackson deserialization for features
 *
 * @author newmanw
 *
 */
public class FeatureResponseBodyConverter implements Converter<ResponseBody, List<StaticFeature>> {

    private final Layer layer;

    public FeatureResponseBodyConverter(Layer layer) {
        this.layer = layer;
    }

    @Override
    public List<StaticFeature> convert(ResponseBody body) throws IOException {
        return new StaticFeatureDeserializer().parseStaticFeatures(body.byteStream(), layer);
    }

}
