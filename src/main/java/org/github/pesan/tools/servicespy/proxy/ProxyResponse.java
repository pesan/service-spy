package org.github.pesan.tools.servicespy.proxy;

public class ProxyResponse {
    private final int status;
    private final long contentLength;
    private final String contentType;

    public ProxyResponse(int status, long contentLength, String contentType) {
        this.status = status;
        this.contentLength = contentLength;
        this.contentType = contentType;
    }

    public int getStatus() {
        return status;
    }

    public long getContentLength() {
        return contentLength;
    }

    public String getContentType() {
        return contentType;
    }
}
