package org.github.pesan.tools.servicespy.proxy;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class RequestContext {
    private final HttpServerRequest serverRequest;
    private final LocalDateTime startTime;
    private final URI requestUri;

    public RequestContext(HttpServerRequest serverRequest, LocalDateTime startTime) {
        this.requestUri = URI.create(serverRequest.uri());
        this.serverRequest = serverRequest;
        this.startTime = startTime;
    }

    public String getRequestPath() {
        return getRequestUri().getPath();
    }

    public String getRequestPathWithQuery() {
        return getRequestUri().getPath() + (getRequestUri().getQuery() != null ? "?" + getRequestUri().getQuery() : "");
    }

    public Map<String, List<String>> getRequestHeaders() {
        return parseHeaders(serverRequest.headers());
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public URI getRequestUri() {
        return requestUri;
    }

    public String getRequestMethod() {
        return serverRequest.method().name();
    }

    public HttpServerResponse getResponse() {
        return serverRequest.response();
    }

    private static Map<String, List<String>> parseHeaders(MultiMap headers) {
        return headers.entries().stream().collect(
                groupingBy(Map.Entry::getKey, mapping(Map.Entry::getValue, toList())));
    }
}