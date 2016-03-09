package org.github.pesan.tools.servicespy.admin;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class RequestLogEntry {
    private final String requestId;
    private final String service;

    private final LocalDateTime startDateTime;
    private final LocalDateTime endDateTime;
    private final long responseTimeMilli;
    private final URL url;
    private final String requestDataIn;
    private final String requestDataOut;
    private final String responseDataIn;
    private final String responseDataOut;
    private final Exception exception;

    public RequestLogEntry(String requestId, String service, URL url) {
        this(requestId, service, LocalDateTime.now(), url, null, null, null, null, null, null);
    }

    private RequestLogEntry(RequestLogEntry entry, Exception exception) {
        this(entry.requestId, entry.service, entry.startDateTime, entry.url, null, null, null, null, exception, LocalDateTime.now());
    }

    private RequestLogEntry(RequestLogEntry entry, String requestDataIn, String requestDataOut, String responseDataInt, String responseDataOut) {
        this(entry.requestId, entry.service, entry.startDateTime, entry.url, requestDataIn, requestDataOut, responseDataInt, responseDataOut, null, LocalDateTime.now());
    }

    private RequestLogEntry(String requestId, String service, LocalDateTime startDateTime, URL url, String requestDataIn, String requestDataOut, String responseDataInt, String responseDataOut, Exception exception, LocalDateTime endDateTime) {
        this.requestId = requestId;
        this.service = service;
        this.startDateTime = startDateTime;
        this.url = url;
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

    public String getService() {
        return service;
    }

    public String getHost() {
        return url.getHost() + (url.getPort() != -1 ? ":"+url.getPort() : url.getDefaultPort() != -1 ? ":"+url.getDefaultPort() : "");
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

    public RequestLogEntry endRequest(String requestDataIn, String requestDataOut, String responseDataIn, String responseDataOut) {
        return new RequestLogEntry(this, requestDataIn, requestDataOut, responseDataIn, responseDataOut);
    }

    public RequestLogEntry endRequest(Exception exception) {
        return new RequestLogEntry(this, exception);
    }

}
