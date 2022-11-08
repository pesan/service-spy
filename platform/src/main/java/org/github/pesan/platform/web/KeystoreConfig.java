package org.github.pesan.platform.web;

import java.util.function.Function;

public sealed interface KeystoreConfig {

    record JksFile(String keystorePath, String password) implements KeystoreConfig {
        public <T> T handle(Function<JksFile, T> jksFile, Function<PfxFile, T> pfxFile,
                            Function<PemFile, T> pemFile, Function<PemData, T> pemData) {
            return jksFile.apply(this);
        }
    }

    record PfxFile(String keystorePath, String password) implements KeystoreConfig {
        public <T> T handle(Function<JksFile, T> jksFile, Function<PfxFile, T> pfxFile,
                            Function<PemFile, T> pemFile, Function<PemData, T> pemData) {
            return pfxFile.apply(this);
        }
    }

    record PemFile(String keyPath, String certPath) implements KeystoreConfig {
        public <T> T handle(Function<JksFile, T> jksFile, Function<PfxFile, T> pfxFile,
                            Function<PemFile, T> pemFile, Function<PemData, T> pemData) {
            return pemFile.apply(this);
        }
    }

    record PemData(byte[] keyData, byte[] certData) implements KeystoreConfig {
        public <T> T handle(Function<JksFile, T> jksFile, Function<PfxFile, T> pfxFile,
                            Function<PemFile, T> pemFile, Function<PemData, T> pemData) {
            return pemData.apply(this);
        }
    }

    <T> T handle(Function<JksFile, T> jksFile, Function<PfxFile, T> pfxFile,
                 Function<PemFile, T> pemFile, Function<PemData, T> pemData);
}
