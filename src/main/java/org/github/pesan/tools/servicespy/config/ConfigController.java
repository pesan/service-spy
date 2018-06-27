package org.github.pesan.tools.servicespy.config;

import io.reactivex.Completable;
import io.reactivex.Single;
import org.github.pesan.tools.servicespy.proxy.ProxyProperties;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private static final List<String> SUPPORTED_PROTOCOLS = asList("http", "https");
    private final ProxyProperties config;

    public ConfigController(ProxyProperties config) {
        this.config = config;
    }

    @RequestMapping(method=RequestMethod.GET)
    public Single<ProxyProperties> get() {
        return Single.just(config);
    }

    @RequestMapping(method=RequestMethod.PUT)
    @ResponseStatus(NO_CONTENT)
    public Completable put(@RequestBody ProxyProperties newConfig) {
        return get()
                .doOnSuccess(config -> {
                    for (Map.Entry<String, ProxyServer> newProxyServer : newConfig.getServers().entrySet()) {
                        ProxyServer proxyServer = config.getServers().get(newProxyServer.getKey());

                        if (proxyServer == null) {
                            throw new ResponseStatusException(BAD_REQUEST, format("unknown proxy server: %s", newProxyServer.getKey()));
                        }

                        boolean valid = newProxyServer.getValue().getMappings().stream()
                                                      .allMatch(ConfigController::hasSupportedProtocols);
                        if (!valid) {
                            throw new ResponseStatusException(BAD_REQUEST,
                                    format("expected protocol from: %s", String.join(", ", SUPPORTED_PROTOCOLS))
                            );
                        }

                        proxyServer.getMappings().clear();
                        proxyServer.getMappings().addAll(newProxyServer.getValue().getMappings());
                    }
                })
                .ignoreElement();
    }

    private static boolean hasSupportedProtocols(ProxyServer.Mapping mapping) {
        return SUPPORTED_PROTOCOLS.stream()
                                  .anyMatch(protocol -> mapping.getUrl().toLowerCase().startsWith(protocol));
    }
}
