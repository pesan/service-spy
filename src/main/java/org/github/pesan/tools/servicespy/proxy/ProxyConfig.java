package org.github.pesan.tools.servicespy.proxy;

import static java.util.stream.Collectors.toList;
import static org.springframework.util.FileCopyUtils.copyToByteArray;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PfxOptions;

import java.io.IOException;

import org.github.pesan.tools.servicespy.config.ProxyServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

@Configuration
public class ProxyConfig {

    private @Autowired ResourceLoader resourceLoader;

    @Bean
    public Vertx vertx() {
        return Vertx.vertx();
    }

    @Bean
    public HttpServerBindings proxyServers(Vertx vertx, ProxyProperties proxyProperties) {
        return new HttpServerBindings(proxyProperties.getServers().entrySet().stream()
            .map(serverSetting -> {
                    ProxyServer server = serverSetting.getValue();
                    return new HttpServerBindings.Binding(
                            serverSetting.getKey(),
                            vertx.createHttpServer(createServerOptions(vertx, server, proxyProperties)),
                            server.getHost(),
                            server.getPort(),
                            server.getMappings()
                    );
            })
            .collect(toList()));
    }

    private HttpServerOptions createServerOptions(Vertx vertx, ProxyServer value, ProxyProperties proxyProperties) {
        HttpServerOptions options = new HttpServerOptions()
            .setHost(value.getHost())
            .setPort(value.getPort())
            .setSsl(value.getSsl());
        if (!StringUtils.isEmpty(value.getJksKeystore())) {
            options.setKeyStoreOptions(
                new JksOptions()
                .setPassword(value.getJksPassword())
                .setPath(value.getJksKeystore())
            );
        } else if (!StringUtils.isEmpty(value.getPemKeyPath())) {
            options.setPemKeyCertOptions(
                new PemKeyCertOptions()
                .setCertPath(value.getPemCertPath())
                .setKeyPath(value.getPemKeyPath())
            );
        } else if (!StringUtils.isEmpty(value.getPfxKeystore())) {
            options.setPfxKeyCertOptions(
                new PfxOptions()
                .setPassword(value.getPfxPassword())
                .setPath(value.getPfxKeystore())

            );
        } else if (value.getSsl()) {
            options.setPemKeyCertOptions(
                new PemKeyCertOptions()
                .setCertValue(resourceToBuffer("classpath:tls/default-cert.pem"))
                .setKeyValue(resourceToBuffer("classpath:tls/default-key.pem"))
            );
        }
        return options;
    }

    @Bean
    public HttpClientOptions httpClientOptions(ProxyProperties proxyProperties) {
        return new HttpClientOptions()
                .setIdleTimeout(proxyProperties.getIdleTimeout() / 1000)
                .setConnectTimeout(proxyProperties.getConnectionTimeout());
    }

    @Bean
    @Qualifier("https")
    public HttpClientOptions httpsClientOptions(ProxyProperties proxyProperties) {
        return new HttpClientOptions()
                .setIdleTimeout(proxyProperties.getIdleTimeout() / 1000)
                .setConnectTimeout(proxyProperties.getConnectionTimeout())
                .setSsl(true)
                .setTrustAll(true)
                .setVerifyHost(false);
    }

    @Bean
    public HttpClient httpClient(Vertx vertx, HttpClientOptions httpClientOptions) {
        return vertx.createHttpClient(httpClientOptions);
    }

    @Bean
    @Qualifier("https")
    public HttpClient httpsClient(Vertx vertx, @Qualifier("https") HttpClientOptions httpsClientOptions) {
        return vertx.createHttpClient(httpsClientOptions);
    }

    private Buffer resourceToBuffer(String path) {
        try {
            Resource resource = resourceLoader.getResource(path);
            return Buffer.buffer(
                    copyToByteArray(resource.getInputStream())
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

