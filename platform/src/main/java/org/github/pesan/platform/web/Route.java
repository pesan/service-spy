package org.github.pesan.platform.web;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

public class Route {
    private final String method;
    private final RequestPath path;
    private final RequestHandler handler;
    private final @Nullable String consumesOrNull;
    private final @Nullable String producesOrNull;

    public Route(String method, String path, RequestHandler handler) {
        this(method, RequestPath.fromString(path), handler, null, null);
    }

    public Route(String method,
                 RequestPath path,
                 RequestHandler handler,
                 @Nullable String consumesOrNull,
                 @Nullable String producesOrNull) {
        this.method = method;
        this.path = path;
        this.handler = handler;
        this.consumesOrNull = consumesOrNull;
        this.producesOrNull = producesOrNull;
    }

    public Route prefix(RequestPath prefix) {
        return new Route(method, path.prefix(prefix), handler, consumesOrNull, producesOrNull);
    }

    public Route consumes(String consumes) {
        return new Route(method, path, handler, consumes, producesOrNull);
    }

    public Route produces(String produces) {
        return new Route(method, path, handler, consumesOrNull, produces);
    }

    public String getMethod() {
        return method;
    }

    public RequestPath getPath() {
        return path;
    }

    public RequestHandler getHandler() {
        return handler;
    }

    public Optional<String> getProduces() {
        return Optional.ofNullable(producesOrNull);
    }

    public Optional<String> getConsumes() {
        return Optional.ofNullable(consumesOrNull);
    }

    public String build(Map<String, String> variables) {
        return path.asPath(variables);
    }
}
