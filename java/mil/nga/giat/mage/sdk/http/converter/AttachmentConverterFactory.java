package mil.nga.giat.mage.sdk.http.converter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import mil.nga.giat.mage.sdk.datastore.observation.Attachment;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * Retrofit converter factory for attachments
 *
 * @author newmanw
 *
 */
public final class AttachmentConverterFactory extends Converter.Factory {

    public static AttachmentConverterFactory create() {
        return new AttachmentConverterFactory();
    }

    private AttachmentConverterFactory() {
    }

    @Override
    public Converter<ResponseBody, Attachment> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
        return new AttachmentResponseBodyConverter();
    }
}
