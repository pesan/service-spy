package org.github.pesan.tools.servicespy.action.entry;

import java.net.URL;
import java.time.LocalDateTime;

public interface ResponseEntry {
    LocalDateTime getTime();
    URL getUrl();

    default String getHost() {
        URL url = getUrl();
        if (url == null) return "";
        return url.getHost() + (url.getPort() != -1 ? ":"+url.getPort() : url.getDefaultPort() != -1 ? ":"+url.getDefaultPort() : "");
    }

    default String getHostName() {
        URL url = getUrl();
        if (url == null) return "";
        return url.getHost();
    }

    default int getPort() {
        URL url = getUrl();
        if (url == null) return 0;
        return url.getPort() != -1 ? url.getPort() : url.getDefaultPort() != -1 ? url.getDefaultPort() : 0;
    }

}
