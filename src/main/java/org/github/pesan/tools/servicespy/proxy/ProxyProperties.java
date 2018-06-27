package org.github.pesan.tools.servicespy.proxy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.github.pesan.tools.servicespy.config.ProxyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix="proxy")
public class ProxyProperties {

    private static final Logger logger = LoggerFactory.getLogger(ProxyProperties.class);

    private int connectionTimeout = 5000;
    private int idleTimeout = 5000;
    private final Map<String, ProxyServer> servers = new LinkedHashMap<>();
    private final List<ProxyServer.Mapping> mappings = new ArrayList<>();

    @JsonIgnore
    public int getConnectionTimeout() { return connectionTimeout; }
    public void setConnectionTimeout(int connectionTimeout) { this.connectionTimeout = connectionTimeout; }

    @JsonIgnore
    public int getIdleTimeout() { return idleTimeout; }
    public void setIdleTimeout(int idleTimeout) { this.idleTimeout = idleTimeout; }

    public Map<String, ProxyServer> getServers() { return servers; }

    /**
     * @deprecated see {@link #getMappings()}
     */
    @Deprecated
    @PostConstruct
    public void init() {
        if (!mappings.isEmpty()) {
            logger.warn("*******************************************************************************************'");
            logger.warn("* DEPRECATION WARNING!");
            logger.warn("* Adding {} mapping(s) to all servers through deprecated configuration property: \'proxy.mappings\'", mappings.size());
            logger.warn("* Use \'proxy.servers.<server-name>.mappings\' to define mappings per server instead.");
            logger.warn("*******************************************************************************************'");
            servers.values().stream()
                   .map(ProxyServer::getMappings)
                   .forEach(server -> server.addAll(mappings));
        }
    }

    /**
     * @deprecated for backwards compatibility. use {@link ProxyServer#getMappings()} instead
     */
    @Deprecated
    public List<ProxyServer.Mapping> getMappings() {
        return mappings;
    }
}
