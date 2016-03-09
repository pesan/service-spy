package org.github.pesan.tools.servicespy.admin;

import org.github.pesan.tools.servicespy.common.ProxyConfig;
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

    private @Autowired ProxyConfig config;

    @RequestMapping(method=RequestMethod.GET)
    public Observable<ProxyConfig> get() {
        return Observable.just(config);
    }

    @RequestMapping(method=RequestMethod.PUT)
    public Observable<HttpStatus> put(@RequestBody ProxyConfig newConfig) {
        return Observable.just(newConfig)
                .doOnNext(x -> {
                    config.getMappings().clear();
                    config.getMappings().addAll(newConfig.getMappings());
                    config.getRequestTransforms().clear();
                    config.getRequestTransforms().addAll(newConfig.getRequestTransforms());
                    config.getResponseTransforms().clear();
                    config.getResponseTransforms().addAll(newConfig.getResponseTransforms());
                })
                .map(x -> HttpStatus.NO_CONTENT);
    }
}
