package mil.nga.giat.mage.sdk.retrofit.converter;

import com.squareup.okhttp.ResponseBody;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import mil.nga.giat.mage.sdk.datastore.observation.Attachment;
import retrofit.Converter;

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
    public Converter<ResponseBody, Attachment> fromResponseBody(Type type, Annotation[] annotations) {
        return new AttachmentResponseBodyConverter();
    }
}
