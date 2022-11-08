package org.github.pesan.platform.vertx;

import io.vertx.rxjava3.core.MultiMap;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.core.http.HttpServerRequest;
import org.github.pesan.platform.web.RequestPath;
import org.github.pesan.platform.web.HttpHeaders;
import org.github.pesan.platform.web.WebRequest;

import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

final class Adapter {

    static WebRequest fromServerRequest(HttpServerRequest request, RequestPath matchedPath) {
        return new WebRequest(
                request.method().name(),
                request.uri(),
                fromHeaders(request.headers()),
                request.toFlowable().map(Buffer::getBytes),
                matchedPath);
    }

    static HttpHeaders fromHeaders(MultiMap headers) {
        return HttpHeaders.fromMap(headers.entries().stream()
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        mapping(Map.Entry::getValue, toList()))));
    }
}
