package org.github.pesan.tools.servicespy.config;

import org.github.pesan.tools.servicespy.proxy.ProxyProperties;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import rx.Observable;

import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final ProxyProperties config;

    public ConfigController(ProxyProperties config) {
        this.config = config;
    }

    @RequestMapping(method=RequestMethod.GET)
    public Observable<ProxyProperties> get() {
        return Observable.just(config);
    }

    @RequestMapping(method=RequestMethod.PUT)
    public Observable<HttpStatus> put(@RequestBody ProxyProperties newConfig) {
        return get()
                .map(config -> {
                    for (Map.Entry<String, ProxyServer> newProxyServer : newConfig.getServers().entrySet()) {
                        ProxyServer proxyServer = config.getServers().get(newProxyServer.getKey());
                        boolean valid = proxyServer != null &&
                                newProxyServer.getValue().getMappings().stream()
                                              .allMatch(mapping -> mapping.getUrl().startsWith("http:") || mapping.getUrl().startsWith("https:"));

                        if (!valid) {
                            return HttpStatus.BAD_REQUEST;
                        }

                        proxyServer.getMappings().clear();
                        proxyServer.getMappings().addAll(newProxyServer.getValue().getMappings());
                    }
                    return HttpStatus.NO_CONTENT;
                });
    }
}
