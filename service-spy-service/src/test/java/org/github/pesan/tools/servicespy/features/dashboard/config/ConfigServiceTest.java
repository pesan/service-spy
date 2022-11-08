package org.github.pesan.tools.servicespy.features.dashboard.config;

import org.github.pesan.tools.servicespy.application.ProxyServerDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigServiceTest {

    private ConfigProxyPropertiesDto server1;

    @BeforeEach
    void setup() throws MalformedURLException {
        server1 = new ConfigProxyPropertiesDto(
                1000,
                2000,
                Map.of("server1", new ProxyServerDto(
                        "",
                        1,
                        false,
                        List.of(new ProxyServerDto.MappingDto(
                                "/host/",
                                new URL("http://localhost:9000"),
                                false
                        )),
                        new ProxyServerDto.KeystoreConfig("", "", "", "", "", "")
                ))
        );
    }

    @Test
    void positive() throws MalformedURLException {
        Result<ConfigProxyPropertiesDto, ConfigService.ConfigError> result = new ConfigService().applyConfigUpdate(
                server1,
                new ConfigProxyPropertiesUpdateDto(
                        Map.of("server1",
                                List.of(new ProxyServerDto.MappingDto(
                                        "/.*",
                                        new URL("http://localhost:8000"),
                                        true
                                ))
                        )
                )
        );

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.handle(c -> c, __ -> null).servers().get("server1").mappings())
                .containsExactly(new ProxyServerDto.MappingDto(
                        "/.*",
                        new URL("http://localhost:8000"),
                        true
                ));
    }

    @Test
    void negative() throws MalformedURLException {
        Result<ConfigProxyPropertiesDto, ConfigService.ConfigError> result = new ConfigService().applyConfigUpdate(
                server1,
                new ConfigProxyPropertiesUpdateDto(
                        Map.of("server1",
                                List.of(new ProxyServerDto.MappingDto(
                                        "/.*",
                                        new URL("ftp://localhost:8000"),
                                        true
                                ))
                        )
                )
        );

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.<ConfigService.ConfigError>handle(__ -> null, err -> err))
                .isEqualTo(new ConfigService.ConfigError.BadProtocol(List.of("http", "https"), "ftp"));
    }
}