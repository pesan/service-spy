package org.github.pesan.tools.servicespy.features.dashboard.config;

import org.github.pesan.tools.servicespy.application.ProxyServerDto;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toMap;

class ConfigService {

    private static final List<String> SUPPORTED_PROTOCOLS = asList("http", "https");

    public sealed interface ConfigError {
        record BadProtocol(List<String> supported, String actual) implements ConfigError {

        }
    }

    Result<ConfigProxyPropertiesDto, ConfigError> applyConfigUpdate(ConfigProxyPropertiesDto config,
                                                                    ConfigProxyPropertiesUpdateDto update) {
        return isValidServerConfiguration(update.mappings())
                .map(mappings -> new ConfigProxyPropertiesDto(
                        config.connectionTimeout(),
                        config.idleTimeout(),
                        mappings.entrySet().stream()
                                .collect(toMap(
                                        Map.Entry::getKey,
                                        entry -> {
                                            ProxyServerDto proxyServerDto = config.servers().get(entry.getKey());
                                            return new ProxyServerDto(
                                                    proxyServerDto.host(),
                                                    proxyServerDto.port(),
                                                    proxyServerDto.ssl(),
                                                    entry.getValue(),
                                                    proxyServerDto.keystoreConfig()
                                            );
                                        }
                                ))
                ));
    }

    private Result<Map<String, List<ProxyServerDto.MappingDto>>, ConfigError> isValidServerConfiguration(Map<String, List<ProxyServerDto.MappingDto>> mappings) {
        return mappings.values().stream()
                .flatMap(Collection::stream)
                .filter(ConfigService::hasInvalidProtocol)
                .findFirst()
                .map(mapping -> Result.<Map<String, List<ProxyServerDto.MappingDto>>, ConfigError>error(new ConfigError.BadProtocol(SUPPORTED_PROTOCOLS, mapping.url().getProtocol())))
                .orElseGet(() -> Result.success(mappings));
    }

    private static boolean hasInvalidProtocol(ProxyServerDto.MappingDto mappingDto) {
        return SUPPORTED_PROTOCOLS.stream()
                .noneMatch(protocol -> protocol.equals(mappingDto.url().getProtocol()));
    }
}