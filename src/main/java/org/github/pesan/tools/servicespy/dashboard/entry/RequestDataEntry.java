package org.github.pesan.tools.servicespy.dashboard.entry;

import org.github.pesan.tools.servicespy.dashboard.HttpHeaders;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.time.LocalDateTime;

public class RequestDataEntry {
    private final String requestPath;
    private final String query;
    private final String httpMethod;
    private final String contentType;
    private final HttpHeaders headers;
    private final LocalDateTime time;
    private final ExceptionDetails exception;

    private final ByteArrayOutputStream data;

    public RequestDataEntry(URI requestUri, String httpMethod, HttpHeaders httpHeaders, ByteArrayOutputStream data, LocalDateTime time, ExceptionDetails exception) {
        this.requestPath = requestUri.getPath();
        this.query = requestUri.getQuery() != null ? "?" + requestUri.getQuery() : "";
        this.httpMethod = httpMethod;
        this.contentType = httpHeaders.getFirstHeader("Content-Type").orElse("");
        this.headers = httpHeaders;
        this.data = data;
        this.time = time;
        this.exception = exception;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public String getQuery() {
        return query;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

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