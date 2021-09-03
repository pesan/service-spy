package org.github.pesan.tools.servicespy.action.entry;


import org.github.pesan.tools.servicespy.action.RequestId;

import java.time.ZoneOffset;

public class LogEntry {

    private final RequestId id;
    private final RequestDataEntry request;
    private final ResponseDataEntry response;

    public LogEntry(RequestId  id, RequestDataEntry request, ResponseDataEntry response) {
        this.id = id;
        this.request = request;
        this.response = response;
    }

    public RequestId getId() {
        return id;
    }

    public RequestDataEntry getRequest() {
        return request;
    }

    public ResponseDataEntry getResponse() {
        return response;
    }

    public long getResponseTimeMillis() {
        return response.getTime().toInstant(ZoneOffset.UTC).toEpochMilli() - request.getTime().toInstant(ZoneOffset.UTC).toEpochMilli();
    }

}