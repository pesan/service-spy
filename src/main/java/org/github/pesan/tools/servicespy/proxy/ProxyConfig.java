package org.github.pesan.tools.servicespy.proxy;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PfxOptions;
import org.apache.logging.log4j.util.Strings;
import org.github.pesan.tools.servicespy.dashboard.config.ProxyServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

public class ProxyConfig {

    private final Buffer defaultPemCert;
    private final Buffer defaultPemKey;

    public ProxyConfig() {
        this.defaultPemCert = loadResource("/tls/default-cert.pem");
        this.defaultPemKey = loadResource("/tls/default-key.pem");
    }

    public HttpServerOptions createServerOptions(ProxyServer value) {
        HttpServerOptions options = new HttpServerOptions()
            .setHost(value.getHost())
            .setPort(value.getPort())
            .setSsl(value.getSsl());
        if (!Strings.isEmpty(value.getKeystoreConfig().getJksKeystore())) {
            options.setKeyStoreOptions(
                new JksOptions()
                .setPassword(value.getKeystoreConfig().getJksPassword())
                .setPath(value.getKeystoreConfig().getJksKeystore())
            );
        } else if (!Strings.isEmpty(value.getKeystoreConfig().getPemKeyPath())) {
            options.setPemKeyCertOptions(
                new PemKeyCertOptions()
                .setCertPath(value.getKeystoreConfig().getPemCertPath())
                .setKeyPath(value.getKeystoreConfig().getPemKeyPath())
            );
        } else if (!Strings.isEmpty(value.getKeystoreConfig().getPfxKeystore())) {
            options.setPfxKeyCertOptions(
                new PfxOptions()
                .setPassword(value.getKeystoreConfig().getPfxPassword())
                .setPath(value.getKeystoreConfig().getPfxKeystore())

            );
        } else if (value.getSsl()) {
            options.setPemKeyCertOptions(
                new PemKeyCertOptions()
                .setCertValue(defaultPemCert)
                .setKeyValue(defaultPemKey)
            );
        }
        return options;
    }

    public HttpClientOptions httpClientOptions(ProxyProperties proxyProperties) {
        return new HttpClientOptions()
                .setIdleTimeout(proxyProperties.getIdleTimeout() / 1000)
                .setConnectTimeout(proxyProperties.getConnectionTimeout());
    }

    public HttpClientOptions httpsClientOptions(ProxyProperties proxyProperties) {
        return new HttpClientOptions()
                .setIdleTimeout(proxyProperties.getIdleTimeout() / 1000)
                .setConnectTimeout(proxyProperties.getConnectionTimeout())
                .setSsl(true)
                .setTrustAll(true)
                .setVerifyHost(false);
    }

    private static Buffer loadResource(String path) {
        try {
            InputStream resourceAsStream = ProxyConfig.class.getResourceAsStream(path);
            if (resourceAsStream == null) {
                throw new IOException("resource not found: " + path);
            }
            return Buffer.buffer(resourceAsStream.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}