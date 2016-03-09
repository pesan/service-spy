package org.github.pesan.tools.servicespy.proxy;

import java.io.InputStream;
import java.io.OutputStream;

public class ProxyRequest {
    private final InputStream input;
    private final OutputStream output;
    private final long contentLength;
    private final String contentType;
    private String requestUri;
    private String backendUrl;

    public ProxyRequest(InputStream input, OutputStream output, long contentLength, String contentType, String requestUri, String backendUrl) {
        this.input = input;
        this.output = output;
        this.contentLength = contentLength;
        this.contentType = contentType;
        this.requestUri = requestUri;
        this.backendUrl = backendUrl;
    }

    public OutputStream getOutputStream() {
        return output;
    }

    public long getContentLength() {
        return contentLength;
    }

    public String getContentType() {
        return contentType;
    }

    public InputStream getInputStream() {
        return input;
    }

    public String getRequestUri() {
        return requestUri;
    }

    public String getBackendUrl() {
        return backendUrl;
    }
}
