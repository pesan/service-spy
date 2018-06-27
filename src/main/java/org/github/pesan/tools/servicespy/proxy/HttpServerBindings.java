package org.github.pesan.tools.servicespy.proxy;

import io.vertx.core.http.HttpServer;
import org.github.pesan.tools.servicespy.config.ProxyServer;

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
        private final String host;
        private final int port;
        private final List<ProxyServer.Mapping> mappings;

        public Binding(String name, HttpServer server, String host, int port, List<ProxyServer.Mapping> mappings) {
            this.name = name;
            this.server = server;
            this.host = host;
            this.port = port;
            this.mappings = mappings;
        }

        public String getName() {
            return name;
        }

        public HttpServer getServer() {
            return server;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public List<ProxyServer.Mapping> getMappings() {
            return mappings;
        }
    }

}
