package org.github.pesan.platform.web;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableTransformer;

public class WebRequest {

    private final String method;
    private final String uri;
    private final HttpHeaders headers;
    private final Flowable<byte[]> body;
    private final RequestPath matchedPath;

    public WebRequest(String method, String uri, HttpHeaders headers, Flowable<byte[]> body, RequestPath matchedPath) {
        this.method = method;
        this.uri = uri;
        this.headers = headers;
        this.body = body;
        this.matchedPath = matchedPath;
    }

    public String method() {
        return method;
    }

    public String uri() {
        return uri;
    }

    public HttpHeaders headers() {
        return headers;
    }

    public Flowable<byte[]> body() {
        return body;
    }

    public RequestPath matchedPath() {
        return matchedPath;
    }

    public WebRequest compose(FlowableTransformer<? super byte[], ? extends byte[]> composer) {
        return new WebRequest(
                method,
                uri,
                headers,
                body.compose(composer),
                matchedPath
        );
    }
}
