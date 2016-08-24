package org.github.pesan.tools.servicespy.action.entry;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@JsonSerialize(using = ResponseDataEntrySerializer.class)
public class ResponseDataEntry implements ResponseEntry {
    private final int status;
    private final URL url;
    private final Map<String, List<String>> headers;
    private final String contentType;
    private final byte[] responseData;
    private final LocalDateTime time;

    public ResponseDataEntry(int status, String contentType, URL url, Map<String, List<String>> headers, byte[] responseData, LocalDateTime time) {
        this.status = status;
        this.url = url;
        this.headers = headers;
        this.contentType = contentType;
        this.responseData = responseData;
        this.time = time;
    }

    public int getStatus() {
        return status;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getData() {
        return responseData;
    }

    @Override
    public LocalDateTime getTime() {
        return time;
    }
}
