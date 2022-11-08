package org.github.pesan.platform;

import io.reactivex.rxjava3.core.Completable;
import org.github.pesan.platform.messaging.MessageBus;
import org.github.pesan.platform.web.WebPlatform;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Stream;

public interface Platform {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Alias {
        String[] value();
    }

    static Optional<Platform> resolve(String platformSpecifier) {
        List<ServiceLoader.Provider<Platform>> providers = ServiceLoader.load(Platform.class).stream().toList();
        return providers.stream()
                .filter(provider -> Stream.concat(
                                        Stream.of(provider.type().getName()),
                                        Optional.ofNullable(provider.type().getAnnotation(Alias.class))
                                                .stream()
                                                .flatMap(alias -> Arrays.stream(alias.value()))
                                )
                                .anyMatch(platformSpecifier::equals)
                )
                .findFirst()
                .map(ServiceLoader.Provider::get);
    }

    Completable launch(Collection<Feature> features);

    static void unhandled(Throwable throwable) {
        throwable.printStackTrace();
    }

    WebPlatform web();

    MessageBus messageBus();

}
