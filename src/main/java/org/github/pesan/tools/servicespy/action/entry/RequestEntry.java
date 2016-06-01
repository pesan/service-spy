package org.github.pesan.tools.servicespy.action.entry;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.github.pesan.tools.servicespy.action.LocalDateTimeSerializer;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;

public class RequestEntry {
    private final String requestPath;
    private final String requestPathWithQuery;
    private final String httpMethod;
    private final LocalDateTime time;

    private final ByteArrayOutputStream data;

    public RequestEntry(String requestPath, String requestPathWithQuery, String httpMethod, ByteArrayOutputStream data, LocalDateTime time) {
        this.requestPath = requestPath;
        this.requestPathWithQuery = requestPathWithQuery;
        this.httpMethod = httpMethod;
        this.data = data;
        this.time = time;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public String getRequestPathWithQuery() {
        return requestPathWithQuery;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getData() {
        try {
            return data.toString("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    public LocalDateTime getTime() {
        return time;
    }
}
