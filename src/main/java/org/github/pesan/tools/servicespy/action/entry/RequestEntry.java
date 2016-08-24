package org.github.pesan.tools.servicespy.action.entry;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class RequestEntry {
    private final String requestPath;
    private final String requestPathWithQuery;
    private final String httpMethod;
    private final Map<String, List<String>> headers;
    private final LocalDateTime time;

    private final ByteArrayOutputStream data;

    public RequestEntry(URI requestUri, String httpMethod, Map<String, List<String>> headers, ByteArrayOutputStream data, LocalDateTime time) {
        this.requestPath = requestUri.getPath();
        this.requestPathWithQuery = requestUri.getPath() + (requestUri.getQuery() != null ? "?" + requestUri.getQuery() : "");
        this.httpMethod = httpMethod;
        this.headers = headers;
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

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public byte[] getData() {
        return data.toByteArray();
    }

    public LocalDateTime getTime() {
        return time;
    }
}
