package org.github.pesan.tools.servicespy.proxy;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProxyConfig {

    @Bean
    public Vertx vertx() {
        return Vertx.vertx();
    }

    @Bean
    public HttpClientOptions httpClientOptions(ProxyProperties proxyProperties) {
        return new HttpClientOptions()
                .setIdleTimeout(proxyProperties.getIdleTimeout() / 1000)
                .setConnectTimeout(proxyProperties.getConnectionTimeout());
    }

    @Bean
    public HttpClient httpClient(Vertx vertx, HttpClientOptions httpClientOptions) {
        return vertx.createHttpClient(httpClientOptions);
    }

    @Bean
    public HttpServer httpServer(Vertx vertx) {
        return vertx.createHttpServer();
    }

}
