package org.github.pesan.tools.servicespy.action.entry;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.time.ZoneOffset;

@JsonSerialize(using=LogEntrySerializer.class)
public class LogEntry {

    private final String id;
    private final RequestEntry request;
    private final ResponseEntry response;

    public LogEntry(String id, RequestEntry request, ResponseEntry response) {
        this.id = id;
        this.request = request;
        this.response = response;
    }

    public String getId() {
        return id;
    }

    public RequestEntry getRequest() {
        return request;
    }

    public ResponseEntry getResponse() {
        return response;
    }

    public long getResponseTimeMillis() {
        return response.getTime().toInstant(ZoneOffset.UTC).toEpochMilli() - request.getTime().toInstant(ZoneOffset.UTC).toEpochMilli();
    }

}
