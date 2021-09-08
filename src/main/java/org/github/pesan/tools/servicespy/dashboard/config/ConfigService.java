package org.github.pesan.tools.servicespy.dashboard.config;

import io.reactivex.rxjava3.core.Single;
import org.github.pesan.tools.servicespy.proxy.ProxyProperties;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

class ConfigService {

    private static final List<String> SUPPORTED_PROTOCOLS = asList("http", "https");
    private ProxyProperties config;

    ConfigService(ProxyProperties config) {
        this.config = config;
    }

    Single<ProxyProperties> get() {
        return Single.fromSupplier(() -> config);
    }

    Single<ProxyProperties> put(ProxyProperties newConfig) {
        return get()
                .flatMap(config -> Single.fromSupplier(() -> {
                    Map<String, ProxyServer> newServers = newConfig.getServers();

                    isValidServerConfiguration(newServers);

                    this.config = new ProxyProperties(
                            config.getConnectionTimeout(),
                            config.getIdleTimeout(),
                            config.getServers().entrySet().stream()
                                    .map(e -> newServers.containsKey(e.getKey())
                                            ? Map.entry(
                                            e.getKey(),
                                            new ProxyServer(
                                                    e.getValue().getHost(),
                                                    e.getValue().getPort(),
                                                    e.getValue().getSsl(),
                                                    e.getValue().getKeystoreConfig(),
                                                    newServers.get(e.getKey()).getMappings()))
                                            : e
                                    ).collect(toList()));

                    return this.config;
                }));
    }

    private static void isValidServerConfiguration(Map<String, ProxyServer> newServers) {
        Optional<ProxyServer.Mapping> invalidServerOrEmpty = newServers.values().stream().flatMap(e -> e.getMappings().stream())
                .filter(ConfigService::hasInvalidProtocol)
                .findFirst();

        if (invalidServerOrEmpty.isPresent()) {
            throw new RuntimeException(format("expected protocol: %s, for url: %s",
                    String.join("/", SUPPORTED_PROTOCOLS),
                    invalidServerOrEmpty.orElseThrow().getUrl()));
        }
    }

    private static boolean hasInvalidProtocol(ProxyServer.Mapping mapping) {
        return SUPPORTED_PROTOCOLS.stream()
                .noneMatch(protocol -> protocol.equals(mapping.getUrl().getProtocol()));
    }
}