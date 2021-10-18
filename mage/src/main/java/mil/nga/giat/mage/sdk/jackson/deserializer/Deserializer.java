package mil.nga.giat.mage.sdk.jackson.deserializer;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Deserializer {

    static JsonFactory factory = new JsonFactory();
    static ObjectMapper mapper = new ObjectMapper();
    
    static {
        factory.setCodec(mapper);
    }
}
