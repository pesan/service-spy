package org.github.pesan.platform.vertx;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientResponse;
import org.github.pesan.platform.Platform;
import org.github.pesan.platform.web.WebClient;
import org.github.pesan.platform.web.WebRequest;
import org.github.pesan.platform.web.WebResponse;

import static org.github.pesan.platform.vertx.Adapter.fromHeaders;

class VertxWebClient implements WebClient {

    private final HttpClient httpClient;

    VertxWebClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public Single<WebResponse> send(String host, int port, WebRequest request) {
        return httpClient.request(HttpMethod.valueOf(request.method()), port, host, request.uri())
                .flatMap(serverRequest -> {
                    serverRequest.setChunked(!request.headers().has("content-length"));
                    return serverRequest.send(request.body().map(Buffer::new))
                            .flatMap(this::toWebResponse);
                })
                .doOnError(Platform::unhandled);
    }

    private Single<WebResponse> toWebResponse(HttpClientResponse resp) {
        return resp.body()
                .map(body -> new WebResponse(resp.statusCode(),
                        fromHeaders(resp.headers()),
                        Flowable.just(body.getBytes()), false));
    }
}
