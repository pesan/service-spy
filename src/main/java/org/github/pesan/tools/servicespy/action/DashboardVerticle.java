package org.github.pesan.tools.servicespy.action;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import org.github.pesan.tools.servicespy.config.ConfigService;
import org.github.pesan.tools.servicespy.proxy.ProxyProperties;
import org.github.pesan.tools.servicespy.util.Futures;
import org.github.pesan.tools.servicespy.util.RxHttpAdapter.HttpResponse;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.github.pesan.tools.servicespy.util.RxHttpAdapter.rxCompletable;
import static org.github.pesan.tools.servicespy.util.RxHttpAdapter.rxMaybe;
import static org.github.pesan.tools.servicespy.util.RxHttpAdapter.rxServerSentEvents;
import static org.github.pesan.tools.servicespy.util.RxHttpAdapter.rxSingle;

public class DashboardVerticle extends AbstractVerticle {

    private Settings settings;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        EventBus eventBus = vertx.eventBus();
        settings = Json.toSettings(config().getJsonObject("server"));

        Futures.zip(
                        trafficFeature(),
                        configurationFeature(),
                        staticContentFeature(),
                        this::assembleRouter)
                .compose(createServer())
                .onSuccess(httpServer -> {
                    eventBus.publish("dashboard.started", new JsonObject()
                            .put("port", httpServer.actualPort()));
                })
                .<Void>mapEmpty()
                .onComplete(startPromise);
    }

    private Router assembleRouter(Router trafficRouter, Router configRouter, Optional<Router> staticContentRouterOrEmpty) {
        Router rootRouter = Router.router(vertx);
        rootRouter.mountSubRouter("/api/traffic", trafficRouter);
        rootRouter.mountSubRouter("/api/config", configRouter);
        staticContentRouterOrEmpty.ifPresent(staticContentRouter ->
                rootRouter.mountSubRouter("/", staticContentRouter));
        return rootRouter;
    }

    private Function<Router, Future<HttpServer>> createServer() {
        int port = settings.getServerPort();
        return router -> vertx.createHttpServer()
                .requestHandler(router)
                .listen(port);
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
                                .map(HttpResponse::ok)

                ));

        trafficRouter.get("/").produces("text/event-stream")
                .handler(rxServerSentEvents(request -> actionService.subscribe().map(Json::fromLogEntry).map(JsonObject::encode)));
        trafficRouter.get("/:id/data/request/")
                .handler(rxMaybe(request -> {
                    RequestId requestId = RequestId.fromText(request.getParam("id"));
                    return actionService.getRequestData(requestId)
                            .map(content -> HttpResponse.ok(content.getData())
                                    .withHeaders(content.getContentType() != null ? Map.of("Content-Type", content.getContentType()) : Map.of())
                            );
                }));
        trafficRouter.get("/:id/data/response/")
                .handler(rxMaybe(request -> {
                    RequestId requestId = RequestId.fromText(request.getParam("id"));
                    return actionService.getResponseData(requestId)
                            .map(content -> HttpResponse.ok(content.getData())
                                    .withHeaders(content.getContentType() != null ? Map.of("Content-Type", content.getContentType()) : Map.of())
                            );
                }));

        vertx.eventBus().<JsonObject>consumer("request.begin", msg ->
                actionService.onBeginRequest(
                        RequestId.fromText(msg.body().getString("requestId")),
                        Json.toRequestDataEntry(msg.body().getJsonObject("payload")))
        );
        vertx.eventBus().<JsonObject>consumer("request.data", msg ->
                actionService.onRequestData(
                        RequestId.fromText(msg.body().getString("requestId")),
                        msg.body().getBinary("payload"))
        );

        vertx.eventBus().<JsonObject>consumer("response.begin", message ->
                actionService.onResponseBegin(
                        RequestId.fromText(message.body().getString("requestId")),
                        Json.toResponseDataEntry(message.body().getJsonObject("payload")))
        );

        vertx.eventBus().<JsonObject>consumer("response.data", msg ->
                actionService.onResponseData(
                        RequestId.fromText(msg.body().getString("requestId")),
                        msg.body().getBinary("payload"))
        );

        vertx.eventBus().<JsonObject>consumer("response.end", msg ->
                actionService.onEnd(RequestId.fromText(msg.body().getString("requestId")))
        );

        vertx.eventBus().<JsonObject>consumer("response.error", msg ->
                actionService.onResponseError(
                        RequestId.fromText(msg.body().getString("requestId")),
                        Json.toExceptionDetails(msg.body().getJsonObject("payload"))));

        return Future.succeededFuture(trafficRouter);
    }

    private Future<Router> configurationFeature() {
        Router configRouter = Router.router(vertx);
        EventBus eventBus = vertx.eventBus();

        JsonObject conf = config();

        ConfigService configService = new ConfigService(Json.toProxyProperties(conf.getJsonObject("proxy")));

        configRouter.get("/").produces("application/json")
                .handler(rxSingle(request -> configService.get()
                        .map(properties -> HttpResponse.ok(Buffer.buffer(Json.fromProxyProperties(properties).encode())))));
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

    private Future<Optional<Router>> staticContentFeature() {
        return Future.succeededFuture(
                settings.getWebroot()
                        .map(webroot -> {
                            Router router = Router.router(vertx);
                            router.get("/*").handler(StaticHandler.create()
                                    .setAllowRootFileSystemAccess(true)
                                    .setWebRoot(webroot)
                                    .setDirectoryListing(false)
                            );
                            return router;
                        })
        );
    }
}