package org.github.pesan.tools.util;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

public class SslUtils {

    private static final X509TrustManager TRUST_ANY = new X509TrustManager() {
        @Override public void checkClientTrusted(X509Certificate[] xcs, String string) {}
        @Override public void checkServerTrusted(X509Certificate[] xcs, String string) {}
        @Override public X509Certificate[] getAcceptedIssuers() { return null; }
    };

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
