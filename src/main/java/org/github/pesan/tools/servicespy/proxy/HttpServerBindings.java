package org.github.pesan.tools.servicespy.proxy;

import io.vertx.core.http.HttpServer;

import java.util.List;
import java.util.stream.Stream;

public class HttpServerBindings {
    private final List<Binding> bindings;

    public HttpServerBindings(List<Binding> bindings) {
        this.bindings = bindings;
    }

    public Stream<Binding> stream() {
        return bindings.stream();
    }

    public static class Binding {
        private final String name;
        private final HttpServer server;
        private final int port;

        public Binding(String name, HttpServer server, int port) {
            this.name = name;
            this.server = server;
            this.port = port;
        }

        public String getName() {
            return name;
        }

        public HttpServer getServer() {
            return server;
        }

        public int getPort() {
            return port;
        }
    }

}
