package org.github.pesan.tools.servicespy.http;

import io.vertx.core.buffer.Buffer;

import java.util.Map;

public class HttpResponse {
    private final int statusCode;
    private final Buffer buffer;
    private final Map<String, String> headers;

    public HttpResponse(int statusCode, Buffer buffer, Map<String, String> headers) {
        this.statusCode = statusCode;
        this.buffer = buffer;
        this.headers = headers;
    }

    public static HttpResponse ok(Buffer buffer) {
        return new HttpResponse(200, buffer, Map.of());
    }

    public static HttpResponse error() {
        return status(500);
    }

    public static HttpResponse status(int statusCode) {
        return new HttpResponse(statusCode, Buffer.buffer(), Map.of());
    }

    public int statusCode() {
        return statusCode;
    }

    public Buffer body() {
        return buffer;
    }

    public Map<String, String> headers() {
        return headers;
    }

    public HttpResponse withHeaders(Map<String, String> headers) {
        return new HttpResponse(statusCode, buffer, headers);
    }
}