package org.github.pesan.tools.servicespy.action.entry;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class LogEntrySerializer extends JsonSerializer<LogEntry> {

    @Override
    public void serialize(LogEntry entry, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
        gen.writeStartObject();

        gen.writeStringField("id", entry.getId());
        gen.writeNumberField("responseTimeMillis", entry.getResponseTimeMillis());

        gen.writeObjectField("request", entry.getRequest());
        gen.writeObjectField("response", entry.getResponse());

        gen.writeObjectFieldStart("href");
        gen.writeStringField("requestData",
                String.format("/api/actions/%s/data/request/", entry.getId())
        );
        gen.writeStringField("responseData",
                String.format("/api/actions/%s/data/response/", entry.getId())
        );
        gen.writeEndObject();


        gen.writeEndObject();
    }


}
