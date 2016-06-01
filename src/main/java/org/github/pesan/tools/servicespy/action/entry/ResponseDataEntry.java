package org.github.pesan.tools.servicespy.action.entry;

import java.net.URL;
import java.time.LocalDateTime;

public class ResponseDataEntry implements ResponseEntry {
    private final int status;
    private final URL url;
    private final String contentType;
    private final String responseData;
    private final LocalDateTime time;

    public ResponseDataEntry(int status, String contentType, URL url, String responseData, LocalDateTime time) {
        this.status = status;
        this.url = url;
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

    public String getContentType() {
        return contentType;
    }

    public String getData() {
        return responseData;
    }

    @Override
    public LocalDateTime getTime() {
        return time;
    }
}
