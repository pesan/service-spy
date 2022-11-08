package org.github.pesan.platform.web;

import io.reactivex.rxjava3.core.Single;

@FunctionalInterface
public interface RequestHandler {
    Single<WebResponse> handle(WebRequest request);
}
