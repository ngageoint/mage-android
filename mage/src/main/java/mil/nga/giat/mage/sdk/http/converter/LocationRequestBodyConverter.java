package mil.nga.giat.mage.sdk.http.converter;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import mil.nga.giat.mage.sdk.datastore.location.Location;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import retrofit2.Converter;

/**
 * Retrofit location request body converter
 *
 * Handles GSON serialization for locations
 *
 * @author newmanw
 *
 */
public class LocationRequestBodyConverter implements Converter<List<Location>, RequestBody> {

    private static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=UTF-8");
    private static final Charset UTF_8 = StandardCharsets.UTF_8;

    private final Gson gson;
    private final Type type;

    LocationRequestBodyConverter(Gson gson, Type type) {
        this.gson = gson;
        this.type = type;
    }

    @Override
    public RequestBody convert(List<Location> value) throws IOException {
        Buffer buffer = new Buffer();
        Writer writer = new OutputStreamWriter(buffer.outputStream(), UTF_8);
        try {
            gson.toJson(value, type, writer);
            writer.flush();
        } catch (IOException e) {
            throw new AssertionError(e); // Writing to Buffer does no I/O.
        }
        return RequestBody.create(MEDIA_TYPE, buffer.readByteString());
    }
}
