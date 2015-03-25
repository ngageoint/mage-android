package mil.nga.giat.mage.sdk.jackson.deserializer;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.io.InputStream;

import mil.nga.giat.mage.sdk.datastore.observation.Attachment;

public class AttachmentDeserializer extends Deserializer {

    public Attachment parseAttachment(InputStream is) throws JsonParseException, IOException {
        JsonParser parser = factory.createParser(is);
        parser.nextToken();

        Attachment attachment = parseAttachment(parser);

        parser.close();
        return attachment;
    }

    Attachment parseAttachment(JsonParser parser) throws JsonParseException, IOException {
        Attachment attachment = new Attachment();
        attachment.setDirty(false);

        if (parser.getCurrentToken() != JsonToken.START_OBJECT)
            return attachment;

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String name = parser.getCurrentName();
            if ("id".equals(name)) {
                parser.nextToken();
                attachment.setRemoteId(parser.getText());
            } else if ("contentType".equals(name)) {
                parser.nextToken();
                attachment.setContentType(parser.getText());
            } else if ("size".equals(name)) {
                parser.nextToken();
                attachment.setSize(parser.getLongValue());
            } else if ("name".equals(name)) {
                parser.nextToken();
                attachment.setName(parser.getText());
            } else if ("relativePath".equals(name)) {
                parser.nextToken();
                attachment.setRemotePath(parser.getText());
            } else if ("url".equals(name)) {
                parser.nextToken();
                attachment.setUrl(parser.getText());
            } else {
                parser.nextToken();
                parser.skipChildren();
            }
        }

        return attachment;
    }
}