package mil.nga.giat.mage.sdk.http.converter;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import retrofit2.Converter;

/**
 * Retrofit observation request body converter
 *
 * Handles GSON serialization for an observation
 *
 * @author newmanw
 *
 */
public class ObservationRequestBodyConverter implements Converter<Observation, RequestBody> {

    private static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=UTF-8");
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private final Gson gson;
    private final Type type;

    ObservationRequestBodyConverter(Gson gson, Type type) {
        this.gson = gson;
        this.type = type;
    }

    @Override
    public RequestBody convert(Observation observation) throws IOException {
        Buffer buffer = new Buffer();
        Writer writer = new OutputStreamWriter(buffer.outputStream(), UTF_8);
        try {
            gson.toJson(observation, type, writer);
            writer.flush();
        } catch (IOException e) {
            throw new AssertionError(e); // Writing to Buffer does no I/O.
        }
        return RequestBody.create(MEDIA_TYPE, buffer.readByteString());
    }
}
