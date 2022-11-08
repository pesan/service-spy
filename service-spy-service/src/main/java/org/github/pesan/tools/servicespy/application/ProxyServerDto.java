package org.github.pesan.tools.servicespy.application;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static java.util.Objects.requireNonNullElseGet;

public record ProxyServerDto(String host,
                             int port,
                             boolean ssl,
                             List<MappingDto> mappings,
                             KeystoreConfig keystoreConfig) {

    @JsonCreator
    public static ProxyServerDto create(@JsonProperty("host") String host,
                                        @JsonProperty("port") int port,
                                        @JsonProperty("ssl") Boolean ssl,
                                        @JsonProperty("mappings") List<MappingDto> mappings,
                                        @JsonProperty("keystoreConfig") KeystoreConfig keystoreConfig) {
        return new ProxyServerDto(requireNonNullElse(host, "0.0.0.0"),
                port,
                requireNonNullElse(ssl, false),
                requireNonNull(mappings),
                requireNonNullElseGet(keystoreConfig, KeystoreConfig::empty)
        );
    }

    public record KeystoreConfig(String jksKeystore,
                                 String jksPassword,
                                 String pfxKeystore,
                                 String pfxPassword,
                                 String pemKeyPath,
                                 String pemCertPath) {
        @JsonCreator
        public static KeystoreConfig create(
                @JsonProperty("jksKeystore") String jksKeystore,
                @JsonProperty("jksPassword") String jksPassword,
                @JsonProperty("pfxKeystore") String pfxKeystore,
                @JsonProperty("pfxPassword") String pfxPassword,
                @JsonProperty("pemKeyPath") String pemKeyPath,
                @JsonProperty("pemCertPath") String pemCertPath

        ) {
            return new KeystoreConfig(
                    requireNonNullElse(jksKeystore, ""),
                    requireNonNullElse(jksPassword, ""),
                    requireNonNullElse(pfxKeystore, ""),
                    requireNonNullElse(pfxPassword, ""),
                    requireNonNullElse(pemKeyPath, ""),
                    requireNonNullElse(pemCertPath, "")
            );
        }

        public static KeystoreConfig empty() {
            return create("", "", "", "", "", "");
        }
    }

    public record MappingDto(String pattern,
                             URL url,
                             boolean active) {
        @JsonCreator
        public static MappingDto create(
                @JsonProperty("pattern") String pattern,
                @JsonProperty("url") URL url,
                @JsonProperty("active") Boolean active) {
            return new MappingDto(
                    requireNonNull(Pattern.compile(pattern).pattern()),
                    requireNonNull(url),
                    requireNonNullElse(active, true)
            );
        }
    }
}