package org.github.pesan.tools.servicespy.config;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ProxyServer {
    private String host = "0.0.0.0";
    private int port = -1;
    private boolean ssl = false;
    private String jksKeystore;
    private String jksPassword;
    private String pfxKeystore;
    private String pfxPassword;
    private String pemKeyPath;
    private String pemCertPath;
    private List<Mapping> mappings = new ArrayList<>();

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public boolean getSsl() { return ssl; }
    public void setSsl(boolean ssl) { this.ssl = ssl; }

    public List<Mapping> getMappings() { return mappings; }

    @JsonIgnore
    public String getJksKeystore() { return jksKeystore; }
    public void setJksKeystore(String jksKeystore) { this.jksKeystore = jksKeystore; }

    @JsonIgnore
    public String getJksPassword() { return jksPassword; }
    public void setJksPassword(String jksPassword) { this.jksPassword = jksPassword; }

    @JsonIgnore
    public String getPfxKeystore() { return pfxKeystore; }
    public void setPfxKeystore(String pfxKeystore) { this.pfxKeystore = pfxKeystore; }

    @JsonIgnore
    public String getPfxPassword() { return pfxPassword; }
    public void setPfxPassword(String pfxPassword) { this.pfxPassword = pfxPassword; }

    @JsonIgnore
    public String getPemKeyPath() { return pemKeyPath; }
    public void setPemKeyPath(String pemKeyPath) { this.pemKeyPath = pemKeyPath; }

    @JsonIgnore
    public String getPemCertPath() { return pemCertPath; }
    public void setPemCertPath(String pemCertPath) { this.pemCertPath = pemCertPath; }

    public static class Mapping {
        private Pattern pattern;
        private String url;
        private boolean active = true;

        public Pattern getPattern() { return pattern; }
        public void setPattern(String pattern) { this.pattern = Pattern.compile(pattern); }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }

}
