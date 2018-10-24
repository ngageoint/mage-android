package mil.nga.giat.mage.sdk.http.converter;

import java.io.IOException;

import mil.nga.giat.mage.sdk.datastore.observation.Attachment;
import mil.nga.giat.mage.sdk.jackson.deserializer.AttachmentDeserializer;
import okhttp3.ResponseBody;
import retrofit2.Converter;

/**
 * Retrofit attachment response body converter
 *
 * Handles Jackson deserialization for attachments
 *
 * @author newmanw
 *
 */
public class AttachmentResponseBodyConverter implements Converter<ResponseBody, Attachment> {

    @Override
    public Attachment convert(ResponseBody body) throws IOException {
        return new AttachmentDeserializer().parseAttachment(body.byteStream());
    }
}
