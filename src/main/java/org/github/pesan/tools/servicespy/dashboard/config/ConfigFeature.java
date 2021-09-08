package org.github.pesan.tools.servicespy.dashboard.config;

import io.reactivex.rxjava3.core.Completable;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import org.github.pesan.tools.servicespy.dashboard.Json;
import org.github.pesan.tools.servicespy.http.HttpResponse;

import static org.github.pesan.tools.servicespy.http.RxHttpAdapter.rxCompletable;
import static org.github.pesan.tools.servicespy.http.RxHttpAdapter.rxSingle;

public class ConfigFeature {
    private final Vertx vertx;
    private final JsonObject config;

    public ConfigFeature(Vertx vertx, JsonObject config) {
        this.vertx = vertx;
        this.config = config;
    }

    public Future<Router> router() {
        Router configRouter = Router.router(vertx);
        EventBus eventBus = vertx.eventBus();

        ConfigService configService = new ConfigService(Json.toProxyProperties(config.getJsonObject("proxy")));

        configRouter.get("/").produces("application/json")
                .handler(rxSingle(request -> configService.get()
                        .map(properties -> HttpResponse.ok(Buffer.buffer(Json.fromProxyProperties(properties).encode())))));
        configRouter.put("/").consumes("application/json")
                .handler(rxCompletable(request ->
                        request.body()
                                .map(buffer -> Json.toProxyProperties(buffer.toJsonObject()))
                                .flatMapSingle(configService::put)
                                .flatMapCompletable(updatedProperties -> Completable.fromPublisher(subscriber -> {
                                            eventBus.request("proxy.settings", Json.fromProxyProperties(updatedProperties), result -> {
                                                if (result.succeeded()) {
                                                    subscriber.onComplete();
                                                } else {
                                                    subscriber.onError(new IllegalStateException("could not reload"));
                                                }
                                            });
                                        })
                                ))
                );
        return Future.succeededFuture(configRouter);
    }
}