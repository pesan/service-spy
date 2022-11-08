package org.github.pesan.platform.web;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public interface Routes {

    static Routes empty() {
        return Stream::empty;
    }

    static Routes of(Collection<Route> routes) {
        return routes::stream;
    }

    static Routes of(Route route) {
        return () -> Stream.of(route);
    }

    static Routes of(Routes... routes) {
        return () -> Arrays.stream(routes).flatMap(Routes::stream);
    }

    static Routes GET(String path, RequestHandler handler) {
        return of(new Route("GET", path, handler));
    }

    static Routes PUT(String path, RequestHandler handler) {
        return of(new Route("PUT", path, handler));
    }

    default Routes config(UnaryOperator<Route> configurator) {
        return () -> stream().map(configurator);
    }

    default Routes and(Routes additionalRoutes) {
        return () -> Stream.concat(stream(), additionalRoutes.stream());
    }

    default Routes mountedAt(RequestPath path) {
        return config(r -> r.prefix(path));
    }

    Stream<Route> stream();

}
