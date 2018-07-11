package org.github.pesan.tools.servicespy.action.entry;

import java.net.URL;
import java.time.LocalDateTime;

public class ResponseExceptionEntry implements ResponseEntry {
    private final URL url;
    private final ExceptionDetails exception;
    private final LocalDateTime time;

    public ResponseExceptionEntry(URL url, ExceptionDetails exception, LocalDateTime time) {
        this.url = url;
        this.exception = exception;
        this.time = time;
    }

    public ExceptionDetails getException() {
        return exception;
    }

    @Override
    public LocalDateTime getTime() {
        return time;
    }

    @Override
    public URL getUrl() {
        return url;
    }
}
