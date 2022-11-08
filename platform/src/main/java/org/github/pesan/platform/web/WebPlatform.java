package org.github.pesan.platform.web;

import io.reactivex.rxjava3.core.Single;

public interface WebPlatform {
    RequestHandler staticHandler(String webroot);

    Single<WebServer> webServer(String host, int port, RequestHandler handler);

    Single<WebServer> webServer(String host, int port, Routes routes);

    default Single<WebServer> webServer(int port, Routes routes) {
        return webServer("0.0.0.0", port, routes);
    }

    Single<WebServer> secureWebServer(String host, int port, KeystoreConfig keystoreConfig, RequestHandler handler);

    WebClient webClient(int idleTimeout, int connectionTimeout);

    WebClient secureWebClient(int idleTimeout, int connectionTimeout);
}
