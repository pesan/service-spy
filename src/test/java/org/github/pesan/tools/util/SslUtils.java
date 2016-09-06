package org.github.pesan.tools.util;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class SslUtils {

    private static X509TrustManager TRUST_ANY = new X509TrustManager() {
        @Override public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {}
        @Override public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {}
        @Override public X509Certificate[] getAcceptedIssuers() { return null; }
    };

    public static RestTemplate trustAll(RestTemplate rest) {
        rest.setRequestFactory(new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection,
                    String httpMethod) throws IOException {
                if (connection instanceof HttpsURLConnection) {
                    ((HttpsURLConnection)connection).setHostnameVerifier((hostname, session) -> true);
                    ((HttpsURLConnection)connection).setSSLSocketFactory(trustSelfSigned().getSocketFactory());
                }
                super.prepareConnection(connection, httpMethod);
            }
        });
        return rest;
    }

    private static SSLContext trustSelfSigned() {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new TrustManager[] { TRUST_ANY }, null);
            SSLContext.setDefault(ctx);
            return ctx;
        } catch (NoSuchAlgorithmException  | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    private SslUtils() {}
}
