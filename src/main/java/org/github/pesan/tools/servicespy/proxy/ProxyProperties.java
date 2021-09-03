package org.github.pesan.tools.servicespy.proxy;

import org.github.pesan.tools.servicespy.config.ProxyServer;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

public class ProxyProperties {
    private final int connectionTimeout;
    private final int idleTimeout;
    private final Map<String, ProxyServer> servers;

    public ProxyProperties(
            int connectionTimeout,
            int idleTimeout,
            List<Map.Entry<String, ProxyServer>> servers) {
        this.connectionTimeout = connectionTimeout;
        this.idleTimeout = idleTimeout;
        this.servers = Collections.unmodifiableMap(servers.stream().collect(toMap(
                Map.Entry::getKey, Map.Entry::getValue, (a, b) -> { throw new IllegalArgumentException("duplicate server identifiers"); }, LinkedHashMap::new)));
    }

    public int getConnectionTimeout() { return connectionTimeout; }
    public int getIdleTimeout() { return idleTimeout; }
    public Map<String, ProxyServer> getServers() { return servers; }
}