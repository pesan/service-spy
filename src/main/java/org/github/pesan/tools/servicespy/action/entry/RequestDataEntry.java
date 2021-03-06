package org.github.pesan.tools.servicespy.action.entry;

import org.github.pesan.tools.servicespy.proxy.RequestContext;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;

public class RequestDataEntry implements RequestEntry {
    private final String requestPath;
    private final String query;
    private final String httpMethod;
    private final String contentType;
    private final Map<String, List<String>> headers;
    private final LocalDateTime time;
    private final ExceptionDetails exception;

    private final ByteArrayOutputStream data;

    public static RequestDataEntry fromContext(RequestContext context, ByteArrayOutputStream received) {
        return new RequestDataEntry(context.getRequestUri(), context.getRequestMethod(), context.getRequestHeaders(), received, context.getStartTime(), null);
    }

    public static RequestDataEntry fromContext(RequestContext context, Exception exception) {
        return new RequestDataEntry(context.getRequestUri(), context.getRequestMethod(), context.getRequestHeaders(), new ByteArrayOutputStream(), context.getStartTime(), ExceptionDetails.fromThrowable(exception));
    }

    public RequestDataEntry(URI requestUri, String httpMethod, Map<String, List<String>> headers, ByteArrayOutputStream data, LocalDateTime time, ExceptionDetails exception) {
        this.requestPath = requestUri.getPath();
        this.query = requestUri.getQuery() != null ? "?" + requestUri.getQuery() : "";
        this.httpMethod = httpMethod;
        this.contentType = headers.getOrDefault("Content-Type", singletonList("")).get(0);
        this.headers = headers;
        this.data = data;
        this.time = time;
        this.exception = exception;
    }

    @Override
    public String getRequestPath() {
        return requestPath;
    }

    @Override
    public String getQuery() {
        return query;
    }

    @Override
    public String getHttpMethod() {
        return httpMethod;
    }

    @Override
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    @Override
    public LocalDateTime getTime() {
        return time;
    }

    public byte[] getData() {
        return data.toByteArray();
    }

    public String getContentType() {
        return contentType;
    }

    public ExceptionDetails getException() {
        return exception;
    }
}
