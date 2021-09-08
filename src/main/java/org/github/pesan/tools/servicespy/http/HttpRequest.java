package org.github.pesan.tools.servicespy.http;

import io.reactivex.rxjava3.core.Maybe;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;

import java.util.Optional;

public class HttpRequest {
    private final HttpServerRequest request;

    public HttpRequest(HttpServerRequest request) {
        this.request = request;
    }

    public Maybe<Buffer> body() {
        return Maybe.fromCompletionStage(request.body()
                .toCompletionStage());
    }

    public String requireParam(String name) {
        return getParam(name).orElseThrow(() -> new IllegalStateException("param not available: " + name));
    }

    public Optional<String> getParam(String name) {
        return Optional.ofNullable(request.getParam(name));
    }
}