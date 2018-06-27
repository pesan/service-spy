package org.github.pesan.tools.servicespy.action.entry;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class ResponseDataEntrySerializer extends JsonSerializer<ResponseDataEntry> {

    @Override
    public void serialize(ResponseDataEntry value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();

        gen.writeBinaryField("data", value.getData());
        gen.writeStringField("contentType", value.getContentType());
        gen.writeNumberField("status", value.getStatus());

        gen.writeObjectField("time", value.getTime());

        gen.writeStringField("url", value.getUrl().toString());
        gen.writeStringField("host", value.getHost());
        gen.writeStringField("hostName", value.getHostName());
        gen.writeNumberField("port", value.getPort());

        gen.writeObjectField("headers", value.getHeaders());

        gen.writeEndObject();
    }
}
