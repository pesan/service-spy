package org.github.pesan.tools.servicespy;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import org.github.pesan.platform.Feature;
import org.github.pesan.platform.Platform;
import org.github.pesan.tools.servicespy.config.ConfigSource;
import org.github.pesan.tools.servicespy.config.ConfigValue;
import org.github.pesan.tools.servicespy.config.YamlConfig;
import org.github.pesan.tools.servicespy.features.dashboard.DashboardFeature;
import org.github.pesan.tools.servicespy.features.proxy.ProxyFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class ServiceSpy {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceSpy.class);

    private static final Map<String, String> ALIASES = Map.of(
            "dashboard", DashboardFeature.class.getName(),
            "proxy", ProxyFeature.class.getName()
    );

    public static Completable run(Config config) {
        ClassLoader classLoader = ServiceSpy.class.getClassLoader();
        List<Feature> features = config.features().entrySet().stream()
                .map(featureConfigEntry -> {
                    Config.Feature featureConfig = featureConfigEntry.getValue();
                    try {
                        Class<?> featureClass = classLoader.loadClass(
                                Optional.ofNullable(ALIASES.get(featureConfig.feature()))
                                        .orElseGet(featureConfig::feature)
                        );
                        Constructor<?> featureConstructor = Arrays.stream(featureClass.getDeclaredConstructors())
                                .filter(constructor -> constructor.getParameterCount() == 1)
                                .findFirst()
                                .orElseThrow();
                        Object featureParameter = YamlConfig.convert(featureConstructor.getParameterTypes()[0], featureConfig.config());
                        return (Feature) featureConstructor
                                .newInstance(featureParameter);
                    } catch (Exception e) {
                        throw new RuntimeException("failed to load feature: %s".formatted(featureConfigEntry.getKey()), e);
                    }
                })
                .toList();


        return Platform.resolve(config.platform())
                .orElseThrow(() -> new IllegalArgumentException("platform '%s' not available on classpath"))
                .launch(features);
    }

    public static void main(String[] args) {
        LOG.info("Starting Service Spy");

        Config config = readConfiguration(
                resourceConfig("/application.yml"),
                fileConfig("application.yml"),
                argsConfig(args)
        );

        run(config)
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onComplete() {
                        LOG.info("Ready to serve");
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        LOG.error("Startup failed", e);
                    }
                });
    }

    private static ConfigSource fileConfig(String s) {
        return () -> {
            try {
                return YamlConfig.fromYaml(Files.newInputStream(Path.of("application.yml"), StandardOpenOption.READ))
                        .stream();
            } catch (IOException e) {
                return Stream.empty();
            }
        };
    }

    private static ConfigSource resourceConfig(String s) {
        return () -> Stream.ofNullable(ServiceSpy.class.getResourceAsStream(s))
                .flatMap(input -> YamlConfig.fromYaml(input).stream());
    }

    private static Config readConfiguration(ConfigSource... sources) {
        return Arrays.stream(sources)
                .flatMap(ConfigSource::get)
                .reduce(ConfigValue::overrideWith)
                .stream()
                .flatMap(config -> YamlConfig.toObject(Config.class, config).stream())
                .findFirst()
                .orElseThrow();
    }

    private static ConfigSource argsConfig(String[] args) {
        return () -> Arrays.stream(args)
                .filter(arg -> arg.startsWith("--"))
                .map(arg -> arg.split("\\s*=\\s*", 2))
                .map(kv -> kv[0].substring(2) + ": " + (kv.length > 1 ? kv[1] : "true"))
                .map(cfg -> YamlConfig.fromYaml(cfg).orElseThrow(e -> new IllegalArgumentException("cannot parse argument when interpreted as Yaml: " + cfg + "\n" + e)))
                .reduce(ConfigValue::overrideWith).stream();
    }

    public record Config(String platform, Map<String, Feature> features) {
        record Feature(String feature, Map<String, Object> config) {
        }
    }
}