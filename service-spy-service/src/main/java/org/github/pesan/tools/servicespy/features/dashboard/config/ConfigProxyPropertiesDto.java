package org.github.pesan.tools.servicespy.features.dashboard.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.github.pesan.tools.servicespy.application.ProxyServerDto;

import java.util.Map;

import static java.util.Objects.requireNonNullElse;
import static java.util.Objects.requireNonNullElseGet;

public record ConfigProxyPropertiesDto(int connectionTimeout,
                                       int idleTimeout,
                                       Map<String, ProxyServerDto> servers) {
    @JsonCreator
    public static ConfigProxyPropertiesDto create(
            @JsonProperty("connectionTimeout") Integer connectionTimeout,
            @JsonProperty("idleTimeout") Integer idleTimeout,
            @JsonProperty("servers") Map<String, ProxyServerDto> servers) {
        return new ConfigProxyPropertiesDto(
                requireNonNullElse(connectionTimeout, 5000),
                requireNonNullElse(idleTimeout, 5000),
                requireNonNullElseGet(servers, Map::of)
        );
    }
}