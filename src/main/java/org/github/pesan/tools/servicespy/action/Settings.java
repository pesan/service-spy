package org.github.pesan.tools.servicespy.action;

import io.reactivex.rxjava3.annotations.Nullable;

import java.util.Optional;

public class Settings {

    private final int serverPort;
    private final String webroot;

    public Settings(int serverPort, @Nullable String webroot) {
        this.serverPort = serverPort;
        this.webroot = webroot;
    }

    public int getServerPort() {
        return serverPort;
    }

    public Optional<String> getWebroot() {
        return Optional.ofNullable(webroot);
    }
}