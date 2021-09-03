package org.github.pesan.tools.servicespy.action.entry;

import org.github.pesan.tools.servicespy.action.HttpHeaders;

import java.net.URL;
import java.time.LocalDateTime;

public class ResponseDataEntry {
    private final int status;
    private final URL url;
    private final HttpHeaders headers;
    private final String contentType;
    private final byte[] responseData;
    private final LocalDateTime time;
    private final ExceptionDetails exceptionDetails = null; // TODO:

    public ResponseDataEntry(int status, String contentType, URL url, HttpHeaders headers, byte[] responseData, LocalDateTime time) {
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

    public URL getUrl() {
        return url;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getData() {
        return responseData;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public String getHost() {
        URL url = getUrl();
        if (url == null) return "";
        return url.getHost() + (url.getPort() != -1 ? ":"+url.getPort() : url.getDefaultPort() != -1 ? ":"+url.getDefaultPort() : "");
    }

    public String getHostName() {
        URL url = getUrl();
        if (url == null) return "";
        return url.getHost();
    }

    public int getPort() {
        URL url = getUrl();
        if (url == null) return 0;
        return url.getPort() != -1 ? url.getPort() : url.getDefaultPort() != -1 ? url.getDefaultPort() : 0;
    }
}