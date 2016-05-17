package org.github.pesan.tools.servicespy.admin;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class RequestLogEntry {
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
    private final String requestDataIn;
    private final String requestDataOut;
    private final String responseDataIn;
    private final String responseDataOut;
    private final Exception exception;

    public RequestLogEntry(String requestId, String requestPath, String requestPathWithQuery, String httpMethod, URL url) {
        this(requestId, requestPath, requestPathWithQuery, LocalDateTime.now(), 0, httpMethod, url, null, null, null, null, null, null, null);
    }

    private RequestLogEntry(RequestLogEntry entry, Exception exception) {
        this(entry.requestId, entry.requestPath, entry.requestPathWithQuery, entry.startDateTime, 0, entry.httpMethod, entry.url, null, null, null, null, null, exception, LocalDateTime.now());
    }

    private RequestLogEntry(RequestLogEntry entry, int status, String contentType, String requestDataIn, String requestDataOut, String responseDataInt, String responseDataOut) {
        this(entry.requestId, entry.requestPath, entry.requestPathWithQuery, entry.startDateTime, status, entry.httpMethod, entry.url, contentType, requestDataIn, requestDataOut, responseDataInt, responseDataOut, null, LocalDateTime.now());
    }

    private RequestLogEntry(String requestId, String requestPath, String requestPathWithQuery, LocalDateTime startDateTime, int status, String httpMethod, URL url, String contentType, String requestDataIn, String requestDataOut, String responseDataInt, String responseDataOut, Exception exception, LocalDateTime endDateTime) {
        this.requestId = requestId;
        this.requestPath = requestPath;
        this.requestPathWithQuery = requestPathWithQuery;
        this.startDateTime = startDateTime;
        this.status = status;
        this.httpMethod = httpMethod;
        this.url = url;
        this.contentType = contentType;
        this.requestDataIn = requestDataIn;
        this.requestDataOut = requestDataOut;
        this.responseDataIn = responseDataInt;
        this.responseDataOut = responseDataOut;
        this.exception = exception;
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
        return url.getHost() + (url.getPort() != -1 ? ":"+url.getPort() : url.getDefaultPort() != -1 ? ":"+url.getDefaultPort() : "");
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

    public String getRequestDataIn() {
        return requestDataIn;
    }

    public String getRequestDataOut() {
        return requestDataOut;
    }

    public String getResponseDataIn() {
        return responseDataIn;
    }

    public String getResponseDataOut() {
        return responseDataOut;
    }

    public Exception getException() {
        return exception;
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

    public RequestLogEntry endRequest(int status, String contentType, String requestDataIn, String requestDataOut, String responseDataIn, String responseDataOut) {
        return new RequestLogEntry(this, status, contentType, requestDataIn, requestDataOut, responseDataIn, responseDataOut);
    }

    public RequestLogEntry endRequest(Exception exception) {
        return new RequestLogEntry(this, exception);
    }

}
