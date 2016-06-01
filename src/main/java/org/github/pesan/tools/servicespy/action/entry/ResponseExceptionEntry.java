package org.github.pesan.tools.servicespy.action.entry;

import java.net.URL;
import java.time.LocalDateTime;

public class ResponseExceptionEntry implements ResponseEntry {
    private final URL url;
    private final Throwable throwable;
    private final LocalDateTime time;

    public ResponseExceptionEntry(URL url, Throwable throwable, LocalDateTime time) {
        this.url = url;
        this.throwable = throwable;
        this.time = time;
    }

    public Throwable getException() {
        return throwable;
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
