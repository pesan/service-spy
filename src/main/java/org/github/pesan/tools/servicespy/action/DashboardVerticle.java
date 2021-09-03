package org.github.pesan.tools.servicespy.action;

import io.reactivex.rxjava3.annotations.Nullable;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import org.github.pesan.tools.servicespy.action.entry.LogEntry;
import org.github.pesan.tools.servicespy.config.ConfigService;
import org.github.pesan.tools.servicespy.proxy.ProxyProperties;

import java.util.Map;
import java.util.Optional;

import static org.github.pesan.tools.servicespy.action.RxHttpAdapter.rxCompletable;
import static org.github.pesan.tools.servicespy.action.RxHttpAdapter.rxMaybe;
import static org.github.pesan.tools.servicespy.action.RxHttpAdapter.rxServerSentEvents;
import static org.github.pesan.tools.servicespy.action.RxHttpAdapter.rxSingle;

public class DashboardVerticle extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        JsonObject server = config().getJsonObject("server");

        CompositeFuture.all(
                        trafficFeature(),
                        configurationFeature(),
                        staticContentFeature(server.getString("webroot"))
                )
                .map((CompositeFuture results) -> {
                    Router rootRouter = Router.router(vertx);
                    rootRouter.mountSubRouter("/api/traffic", results.resultAt(0));
                    rootRouter.mountSubRouter("/api/config", results.resultAt(1));
                    results.<Optional<Router>>resultAt(2).ifPresent(router ->
                            rootRouter.mountSubRouter("/", router));
                    return rootRouter;
                })
                .compose(router -> vertx.createHttpServer()
                        .requestHandler(router)
                        .listen(server.getInteger("port"))
                )
                .<Void>mapEmpty()
                .onSuccess(startPromise::complete)
                .onFailure(startPromise::fail);
    }

    private Future<Router> trafficFeature() {
        Router trafficRouter = Router.router(vertx);

        int maxEntryCount = config().getJsonObject("actions", new JsonObject()).getInteger("limit", 200);
        ActionService actionService = new ActionService(maxEntryCount);

        trafficRouter.delete()
                .handler(rxCompletable(request -> actionService.clear()));

        trafficRouter.get("/").produces("application/json")
                .handler(rxSingle(request ->
                        actionService.list()
                                .map(Json::fromLogEntry)
                                .toList()
                                .map(entries -> Buffer.buffer(new JsonArray(entries).encode()))
                                .map(RxHttpAdapter.HttpResponse::ok)

                ));

        trafficRouter.get("/").produces("text/event-stream")
                .handler(rxServerSentEvents(request -> actionService.subscribe().map(Json::fromLogEntry).map(JsonObject::encode)));
        trafficRouter.get("/:id/data/request/")
                .handler(rxMaybe(request -> {
                    RequestId requestId = RequestId.fromText(request.getParam("id"));
                    return Maybe.zip(
                            actionService.byId(requestId).map(LogEntry::getRequest),
                            actionService.getRequestData(requestId),
                            (entry, data) -> RxHttpAdapter.HttpResponse.ok(data)
                                    .withHeaders(entry.getContentType() != null ? Map.of("Content-Type", entry.getContentType()) : Map.of())
                    );
                }));
        trafficRouter.get("/:id/data/response/")
                .handler(rxMaybe(request -> {
                    RequestId requestId = RequestId.fromText(request.getParam("id"));
                    return Maybe.zip(
                            actionService.byId(requestId).map(LogEntry::getResponse),
                            actionService.getResponseData(requestId),
                            (entry, data) -> RxHttpAdapter.HttpResponse.ok(data)
                                    .withHeaders(entry.getContentType() != null ? Map.of("Content-Type", entry.getContentType()) : Map.of())
                    );
                }));

        vertx.eventBus().<JsonObject>consumer("request.begin", msg ->
                actionService.onBeginRequest(
                        RequestId.fromText(msg.body().getString("requestId")),
                        Json.toRequestDataEntry(msg.body().getJsonObject("requestData")))
        );
        vertx.eventBus().<JsonObject>consumer("request.data", msg ->
                actionService.onRequestData(
                        RequestId.fromText(msg.body().getString("requestId")),
                        msg.body().getBinary("payload"))
        );

        vertx.eventBus().<JsonObject>consumer("response.begin", message ->
                actionService.onResponseBegin(
                        RequestId.fromText(message.body().getString("requestId")),
                        Json.toResponseDataEntry(message.body().getJsonObject("responseData")))
        );

        vertx.eventBus().<JsonObject>consumer("response.data", msg ->
                actionService.onResponseData(
                        RequestId.fromText(msg.body().getString("requestId")),
                        msg.body().getBinary("payload"))
        );

        vertx.eventBus().<JsonObject>consumer("response.end", msg ->
                actionService.onEnd(RequestId.fromText(msg.body().getString("requestId")))
        );

        return Future.succeededFuture(trafficRouter);
    }

    private Future<Router> configurationFeature() {
        Router configRouter = Router.router(vertx);
        EventBus eventBus = vertx.eventBus();

        JsonObject conf = config();

        ConfigService configService = new ConfigService(Json.toProxyProperties(conf.getJsonObject("proxy")));

        configRouter.get("/").produces("application/json")
                .handler(rxSingle(request -> configService.get()
                        .map(properties -> RxHttpAdapter.HttpResponse.ok(Buffer.buffer(Json.fromProxyProperties(properties).encode())))));
        configRouter.put("/").consumes("application/json")
                .handler(rxCompletable(request ->
                        Single.<ProxyProperties>fromPublisher(subscriber ->
                                        request.bodyHandler(buffer -> {
                                            JsonObject requestBody = buffer.toJsonObject();
                                            subscriber.onNext(Json.toProxyProperties(requestBody));
                                            subscriber.onComplete();
                                        })
                                )
                                .flatMap(configService::put)
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

    private Future<Optional<Router>> staticContentFeature(@Nullable String webroot) {
        if (webroot == null) {
            return Future.succeededFuture(Optional.empty());
        }

        Router router = Router.router(vertx);
        router.get("/*").handler(StaticHandler.create()
                .setAllowRootFileSystemAccess(true)
                .setWebRoot(webroot)
                .setDirectoryListing(false)
        );

        return Future.succeededFuture(Optional.of(router));
    }

    public void log(Message<Object> msg) {
        Object body = msg.body();
        if (body instanceof JsonObject) {
            System.out.printf("%s: %s%n", msg.address(), ((JsonObject) body).encodePrettily());
        } else {
            System.out.printf("%s: %s%n", msg.address(), body);
        }
    }
}