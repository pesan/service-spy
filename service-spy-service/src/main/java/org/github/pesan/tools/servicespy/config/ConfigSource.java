package org.github.pesan.tools.servicespy.config;

import java.util.stream.Stream;

@FunctionalInterface
public
interface ConfigSource {
    static ConfigSource of(ConfigValue... values) {
        return () -> Stream.of(values);
    }

    Stream<ConfigValue> get();
}
