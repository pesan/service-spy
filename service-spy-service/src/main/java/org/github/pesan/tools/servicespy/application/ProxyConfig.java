package org.github.pesan.tools.servicespy.application;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

import static java.util.Objects.requireNonNullElse;
import static java.util.Objects.requireNonNullElseGet;

public record ProxyConfig(int connectionTimeout,
                          int idleTimeout,
                          Map<String, ProxyServerDto> servers) {
    @JsonCreator
    public static ProxyConfig create(
            @JsonProperty("connectionTimeout") Integer connectionTimeout,
            @JsonProperty("idleTimeout") Integer idleTimeout,
            @JsonProperty("servers") Map<String, ProxyServerDto> servers) {
        return new ProxyConfig(
                requireNonNullElse(connectionTimeout, 5000),
                requireNonNullElse(idleTimeout, 5000),
                requireNonNullElseGet(servers, Map::of)
        );
    }
}