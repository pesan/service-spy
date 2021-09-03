package org.github.pesan.tools.servicespy.action;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

import java.util.Map;
import java.util.function.Function;

public class RxHttpAdapter {
    public static Handler<RoutingContext> rxServerSentEvents(Function<HttpServerRequest, Observable<String>> op) {
        return context -> {
            HttpServerResponse serverResponse = context.response()
                    .setChunked(true)
                    .putHeader("Content-Type", "text/event-stream")
                    .putHeader("Connection", "keep-alive")
                    .putHeader("Cache-Control", "no-transform") // no-cache?
                    .putHeader("Access-Control-Allow-Origin", "*");

            Disposable disposable = op.apply(context.request())
                    .subscribe(
                            item -> serverResponse.write("data: " + item + "\n\n"),
                            e -> { e.printStackTrace(); serverResponse.setStatusCode(500); serverResponse.end(); },
                            serverResponse::end
                    );
            serverResponse.endHandler(__ -> disposable.dispose());
        };
    }

    public static class HttpResponse {
        private final int statusCode;
        private final Buffer buffer;
        private final Map<String, String> headers;

        public HttpResponse(int statusCode, Buffer buffer, Map<String, String> headers) {
            this.statusCode = statusCode;
            this.buffer = buffer;
            this.headers = headers;
        }

        public static HttpResponse ok(Buffer buffer) {
            return new HttpResponse(200, buffer, Map.of());
        }

        public static HttpResponse error() {
            return status(500);
        }

        public static HttpResponse status(int statusCode) {
            return new HttpResponse(statusCode, Buffer.buffer(), Map.of());
        }

        public HttpResponse withHeaders(Map<String, String> headers) {
            return new HttpResponse(statusCode, buffer, headers);
        }
    }

    public static Handler<RoutingContext> rxCompletable(Function<HttpServerRequest, Completable> op) {
        return rxSingle(op.andThen(result -> result.toSingleDefault(HttpResponse.status(201))));
    }

    public static Handler<RoutingContext> rxMaybe(Function<HttpServerRequest, Maybe<HttpResponse>> op) {
        return rxSingle(op.andThen(result -> result.defaultIfEmpty(HttpResponse.status(404))));
    }

    public static Handler<RoutingContext> rxSingle(Function<HttpServerRequest, Single<HttpResponse>> op) {
        return context -> {
            HttpServerResponse response = context.response();
            Disposable disposable = op.apply(context.request())
                    .subscribe(result -> {
                        response.setStatusCode(result.statusCode);
                        response.headers().addAll(result.headers);
                        response.send(result.buffer);
                    }, e -> {
                        e.printStackTrace();
                        response.setStatusCode(500);
                        context.end();
                    });
            if (!context.response().ended()) {
                context.response().endHandler(__ -> disposable.dispose());
            }
        };
    }

}