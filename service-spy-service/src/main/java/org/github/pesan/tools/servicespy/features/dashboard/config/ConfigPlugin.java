package org.github.pesan.tools.servicespy.features.dashboard.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.processors.BehaviorProcessor;
import io.reactivex.rxjava3.processors.FlowableProcessor;
import org.github.pesan.platform.Platform;
import org.github.pesan.platform.messaging.MessageBus;
import org.github.pesan.platform.web.Routes;
import org.github.pesan.platform.web.WebRequest;
import org.github.pesan.platform.web.WebResponse;
import org.github.pesan.tools.servicespy.features.dashboard.config.ConfigService.ConfigError.BadProtocol;
import org.github.pesan.tools.servicespy.util.Mappers;

import static org.github.pesan.tools.servicespy.features.dashboard.config.Result.otherwise;
import static org.github.pesan.tools.servicespy.features.dashboard.config.Result.type;

public class ConfigPlugin {

    private final ConfigService configService = new ConfigService();
    private final MessageBus.Publisher<ConfigProxyPropertiesDto> proxyPropertiesSender;

    private final FlowableProcessor<ConfigProxyPropertiesDto> config = BehaviorProcessor.<ConfigProxyPropertiesDto>create().toSerialized();

    public ConfigPlugin(Platform platform) {
        MessageBus messaging = platform.messageBus();
        this.proxyPropertiesSender = messaging.publisher("proxy.settings");
        messaging.consumer(ConfigProxyPropertiesDto.class, "proxy.init")
                .take(1)
                .concatWith(messaging.consumer(ConfigProxyPropertiesDto.class, "proxy.settings"))
                .subscribe(config::onNext);
    }

    public Single<Routes> router() {
        return Single.just(Routes.of(
                Routes.GET("", this::handleGetConfig),
                Routes.PUT("", this::handleUpdateConfig)
        ));
    }

    private Single<WebResponse> handleGetConfig(WebRequest request) {
        return latestConfig().map(WebResponse::json);
    }

    private Single<WebResponse> handleUpdateConfig(WebRequest request) {
        return Single.zip(
                        latestConfig(),
                        getConfigUpdate(request),
                        configService::applyConfigUpdate
                )
                .flatMap(result -> result.handle(
                        config -> proxyPropertiesSender.send(config).toSingle(WebResponse::ok),
                        Result.matching(
                                type(BadProtocol.class, __ -> Single.just(WebResponse.badRequest())),
                                otherwise(() -> Single.just(WebResponse.badRequest()))
                        )
                ));
    }

    private Single<ConfigProxyPropertiesDto> latestConfig() {
        return config.take(1).singleOrError();
    }

    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static Single<ConfigProxyPropertiesUpdateDto> getConfigUpdate(WebRequest request) {
        return Mappers.bufferChunks(request.body())
                .map(s -> mapper.readValue(s, ConfigProxyPropertiesUpdateDto.class));
    }
}