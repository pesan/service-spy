package org.github.pesan.tools.servicespy.dashboard.config;

import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;

public class ProxyServer {
    private final String host;
    private final int port;
    private final boolean ssl;
    private final KeystoreConfig keystoreConfig;
    private final List<Mapping> mappings;

    public ProxyServer(
            String host,
            int port,
            boolean ssl,
            KeystoreConfig keystoreConfig,
            List<Mapping> mappings
    ) {
        this.host = host;
        this.port = port;
        this.ssl = ssl;
        this.keystoreConfig = keystoreConfig;
        this.mappings = List.copyOf(mappings);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean getSsl() {
        return ssl;
    }

    public List<Mapping> getMappings() {
        return mappings;
    }

    public KeystoreConfig getKeystoreConfig() {
        return keystoreConfig;
    }

    public static class KeystoreConfig {
        private final String jksKeystore;
        private final String jksPassword;
        private final String pfxKeystore;
        private final String pfxPassword;
        private final String pemKeyPath;
        private final String pemCertPath;

        public KeystoreConfig(String jksKeystore, String jksPassword, String pfxKeystore, String pfxPassword, String pemKeyPath, String pemCertPath) {
            this.jksKeystore = jksKeystore;
            this.jksPassword = jksPassword;
            this.pfxKeystore = pfxKeystore;
            this.pfxPassword = pfxPassword;
            this.pemKeyPath = pemKeyPath;
            this.pemCertPath = pemCertPath;
        }

        public String getJksKeystore() {
            return jksKeystore;
        }

        public String getJksPassword() {
            return jksPassword;
        }

        public String getPfxKeystore() {
            return pfxKeystore;
        }

        public String getPfxPassword() {
            return pfxPassword;
        }

        public String getPemKeyPath() {
            return pemKeyPath;
        }

        public String getPemCertPath() {
            return pemCertPath;
        }
    }

    public static class Mapping {
        private final Pattern pattern;
        private final URL url;
        private final boolean active;

        public Mapping(Pattern pattern, URL url, boolean active) {
            this.pattern = pattern;
            this.url = url;
            this.active = active;
        }

        public Pattern getPattern() {
            return pattern;
        }

        public URL getUrl() {
            return url;
        }

        public boolean isActive() {
            return active;
        }
    }
}