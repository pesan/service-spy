package org.github.pesan.tools.servicespy.http;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

import java.util.function.Function;

public class RxHttpAdapter {
    public static Handler<RoutingContext> rxServerSentEvents(Function<HttpRequest, Observable<String>> op) {
        return context -> {
            HttpServerResponse serverResponse = context.response()
                    .setChunked(true)
                    .putHeader("Content-Type", "text/event-stream")
                    .putHeader("Connection", "keep-alive")
                    .putHeader("Cache-Control", "no-transform") // no-cache?
                    .putHeader("Access-Control-Allow-Origin", "*");

            Disposable disposable = op.apply(new HttpRequest(context.request()))
                    .subscribe(
                            item -> serverResponse.write("data: " + item + "\n\n"),
                            e -> {
                                e.printStackTrace();
                                serverResponse.setStatusCode(500);
                                serverResponse.end();
                            },
                            serverResponse::end
                    );
            serverResponse.endHandler(__ -> disposable.dispose());
        };
    }

    public static Handler<RoutingContext> rxCompletable(Function<HttpRequest, Completable> op) {
        return rxSingle(op.andThen(result -> result.toSingleDefault(HttpResponse.status(201))));
    }

    public static Handler<RoutingContext> rxMaybe(Function<HttpRequest, Maybe<HttpResponse>> op) {
        return rxSingle(op.andThen(result -> result.defaultIfEmpty(HttpResponse.status(404))));
    }

    public static Handler<RoutingContext> rxSingle(Function<HttpRequest, Single<HttpResponse>> op) {
        return context -> {
            HttpServerResponse response = context.response();
            Disposable disposable = op.apply(new HttpRequest(context.request()))
                    .subscribe(result -> {
                        response.setStatusCode(result.statusCode());
                        response.headers().addAll(result.headers());
                        response.send(result.body());
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