package org.github.pesan.tools.servicespy.action;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class RequestLogEntry {
    private static final URL DEFAULT_URL = null;

    private final String requestId;
    private final String requestPath;
    private final String requestPathWithQuery;

    private final LocalDateTime startDateTime;
    private final LocalDateTime endDateTime;
    private final long responseTimeMilli;
    private final URL url;
    private final int status;
    private final String httpMethod;
    private final String contentType;
    private final String requestData;
    private final String responseData;
    private final Throwable throwable;

    public RequestLogEntry(String requestId, String requestPath, String requestPathWithQuery, String httpMethod) {
        this(requestId, requestPath, requestPathWithQuery, LocalDateTime.now(), 0, httpMethod, null, null, null, null, null, null);
    }

    private RequestLogEntry(RequestLogEntry entry, URL url, Throwable throwable) {
        this(entry.requestId, entry.requestPath, entry.requestPathWithQuery, entry.startDateTime, 0, entry.httpMethod, url, null, null, null, throwable, LocalDateTime.now());
    }

    private RequestLogEntry(RequestLogEntry entry, int status, String contentType, URL url, String requestData, String responseData) {
        this(entry.requestId, entry.requestPath, entry.requestPathWithQuery, entry.startDateTime, status, entry.httpMethod, url, contentType, requestData, responseData, null, LocalDateTime.now());
    }

    private RequestLogEntry(String requestId, String requestPath, String requestPathWithQuery, LocalDateTime startDateTime, int status, String httpMethod, URL url, String contentType, String requestData, String responseData, Throwable throwable, LocalDateTime endDateTime) {
        this.requestId = requestId;
        this.requestPath = requestPath;
        this.requestPathWithQuery = requestPathWithQuery;
        this.startDateTime = startDateTime;
        this.status = status;
        this.httpMethod = httpMethod;
        this.url = url;
        this.contentType = contentType;
        this.requestData = requestData;
        this.responseData = responseData;
        this.throwable = throwable;
        this.endDateTime = endDateTime;
        if (endDateTime != null) {
            this.responseTimeMilli = endDateTime.toInstant(ZoneOffset.UTC).toEpochMilli() - startDateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
        } else {
            this.responseTimeMilli = 0;
        }
    }


    public String getRequestId() {
        return requestId;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public String getRequestPathWithQuery() {
        return requestPathWithQuery;
    }

    public String getHost() {
        if (url == null) return "";
        return url.getHost() + (url.getPort() != -1 ? ":"+url.getPort() : url.getDefaultPort() != -1 ? ":"+url.getDefaultPort() : "");
    }

    public String getHostName() {
        if (url == null) return "";
        return url.getHost();
    }

    public int getPort() {
        if (url == null) return 0;
        return url.getPort() != -1 ? url.getPort() : url.getDefaultPort() != -1 ? url.getDefaultPort() : 0;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public int getStatus() {
        return status;
    }

    public String getContentType() {
        return contentType;
    }

    public String getRequestData() {
        return requestData;
    }

    public String getResponseData() {
        return responseData;
    }

    public Throwable getException() {
        return throwable;
    }

    public String getStartDateTime() {
        return startDateTime.format(DateTimeFormatter.ISO_DATE_TIME);
    }

    public String getEndDateTime() {
        return endDateTime.format(DateTimeFormatter.ISO_DATE_TIME);
    }

    public long getResponseTimeMilli() {
        return responseTimeMilli;
    }

    public RequestLogEntry endRequest(int status, String contentType, URL url, String requestData, String responseData) {
        return new RequestLogEntry(this, status, contentType, url, requestData, responseData);
    }

    public RequestLogEntry endRequest(Throwable throwable) {
        return endRequest(throwable, DEFAULT_URL);
    }

    public RequestLogEntry endRequest(Throwable throwable, URL url) {
        return new RequestLogEntry(this, url, throwable);
    }
}
