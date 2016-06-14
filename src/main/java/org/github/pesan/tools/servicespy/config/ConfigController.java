package org.github.pesan.tools.servicespy.config;

import org.github.pesan.tools.servicespy.proxy.ProxyProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import rx.Observable;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final ProxyProperties config;

    @Autowired
    public ConfigController(ProxyProperties config) {
        this.config = config;
    }

    @RequestMapping(method=RequestMethod.GET)
    public Observable<ProxyProperties> get() {
        return Observable.just(config);
    }

    @RequestMapping(method=RequestMethod.PUT)
    public Observable<HttpStatus> put(@RequestBody ProxyProperties newConfig) {
        return Observable.just(newConfig)
                .doOnNext(x -> {
                    config.getMappings().clear();
                    config.getMappings().addAll(newConfig.getMappings());
                })
                .map(x -> HttpStatus.NO_CONTENT);
    }
}
