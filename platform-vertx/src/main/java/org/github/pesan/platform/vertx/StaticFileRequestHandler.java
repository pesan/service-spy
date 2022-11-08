package org.github.pesan.platform.vertx;

import io.reactivex.rxjava3.core.Single;
import org.github.pesan.platform.web.RequestHandler;
import org.github.pesan.platform.web.WebRequest;
import org.github.pesan.platform.web.WebResponse;

class StaticFileRequestHandler implements RequestHandler {
    private final String webroot;

    StaticFileRequestHandler(String webroot) {
        this.webroot = webroot;
    }

    public String getWebroot() {
        return webroot;
    }

    @Override
    public Single<WebResponse> handle(WebRequest request) {
        throw new UnsupportedOperationException("should not be called");
    }
}
