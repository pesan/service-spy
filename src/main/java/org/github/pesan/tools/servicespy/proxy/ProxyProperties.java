package org.github.pesan.tools.servicespy.proxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.github.pesan.tools.servicespy.config.ProxyServer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Component
@ConfigurationProperties(prefix="proxy")
public class ProxyProperties {

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

    private final List<Mapping> mappings = new ArrayList<>();

    private int connectionTimeout = 5000;
    private int idleTimeout = 5000;
    private final Map<String, ProxyServer> servers = new HashMap<>();

    @JsonIgnore
    public int getConnectionTimeout() { return connectionTimeout; }
    public void setConnectionTimeout(int connectionTimeout) { this.connectionTimeout = connectionTimeout; }

    @JsonIgnore
    public int getIdleTimeout() { return idleTimeout; }
    public void setIdleTimeout(int idleTimeout) { this.idleTimeout = idleTimeout; }

    public List<Mapping> getMappings() { return mappings; }

    public Map<String, ProxyServer> getServers() { return servers; }
}
