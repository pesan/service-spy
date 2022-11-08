package org.github.pesan.tools.servicespy.application.event;

import org.github.pesan.platform.web.HttpHeaders;
import org.github.pesan.tools.servicespy.application.ExceptionDetails;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.Optional;

public record ResponseDataEntry(int status,
                                URL url,
                                HttpHeaders headers,
                                String contentType,
                                byte[] data,
                                LocalDateTime time,
                                Optional<ExceptionDetails> exception) {

    // TODO:
    public String getHost() {
        URL url = url();
        if (url == null) return "";
        return url.getHost() + (url.getPort() != -1 ? ":" + url.getPort() : url.getDefaultPort() != -1 ? ":" + url.getDefaultPort() : "");
    }

    // TODO:
    public String getHostName() {
        URL url = url();
        if (url == null) return "";
        return url.getHost();
    }

    // TODO:
    public int getPort() {
        URL url = url();
        if (url == null) return 0;
        return url.getPort() != -1 ? url.getPort() : url.getDefaultPort() != -1 ? url.getDefaultPort() : 0;
    }

    public ResponseDataEntry fail(ExceptionDetails exceptionDetails) {
        return new ResponseDataEntry(status, url, headers, contentType, data, time, Optional.of(exceptionDetails));
    }
}