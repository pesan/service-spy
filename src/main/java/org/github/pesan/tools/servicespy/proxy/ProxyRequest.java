package org.github.pesan.tools.servicespy.proxy;

import java.io.InputStream;
import java.io.OutputStream;

public class ProxyRequest {
    private final InputStream input;
    private final OutputStream output;
    private final String httpMethod;
    private final long contentLength;
    private final String contentType;
    private String requestUri;
    private String backendUrl;

    public ProxyRequest(InputStream input, OutputStream output, String httpMethod, long contentLength, String contentType, String requestUri, String backendUrl) {
        this.input = input;
        this.output = output;
        this.httpMethod = httpMethod;
        this.contentLength = contentLength;
        this.contentType = contentType;
        this.requestUri = requestUri;
        this.backendUrl = backendUrl;
    }

    public InputStream getInputStream() {
        return input;
    }

    public OutputStream getOutputStream() {
        return output;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public long getContentLength() {
        return contentLength;
    }

    public String getContentType() {
        return contentType;
    }

    public String getRequestUri() {
        return requestUri;
    }

    public String getBackendUrl() {
        return backendUrl;
    }

}
