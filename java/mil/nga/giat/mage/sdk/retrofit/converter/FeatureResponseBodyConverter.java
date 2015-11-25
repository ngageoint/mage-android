package mil.nga.giat.mage.sdk.retrofit.converter;

import com.squareup.okhttp.ResponseBody;

import java.io.IOException;
import java.util.List;

import mil.nga.giat.mage.sdk.datastore.layer.Layer;
import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeature;
import mil.nga.giat.mage.sdk.jackson.deserializer.StaticFeatureDeserializer;
import retrofit.Converter;

/**
 * Retrofit location response body converter
 *
 * Handles Jackson deserialization for locations
 *
 * @author newmanw
 *
 */
public class FeatureResponseBodyConverter implements Converter<ResponseBody, List<StaticFeature>> {

    private Layer layer;

    public FeatureResponseBodyConverter(Layer layer) {
        this.layer = layer;
    }

    @Override
    public List<StaticFeature> convert(ResponseBody body) throws IOException {
        return new StaticFeatureDeserializer().parseStaticFeatures(body.byteStream(), layer);
    }

}
