package org.github.pesan.platform.web;

import io.reactivex.rxjava3.core.Single;

public interface WebClient {
    Single<WebResponse> send(String host, int port, WebRequest request);
}
