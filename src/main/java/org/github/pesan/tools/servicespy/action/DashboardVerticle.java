package org.github.pesan.tools.servicespy.action;

import io.reactivex.rxjava3.annotations.Nullable;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import org.github.pesan.tools.servicespy.action.entry.LogEntry;
import org.github.pesan.tools.servicespy.action.entry.RequestDataEntry;
import org.github.pesan.tools.servicespy.action.entry.ResponseDataEntry;
import org.github.pesan.tools.servicespy.config.ConfigService;
import org.github.pesan.tools.servicespy.proxy.ProxyProperties;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.github.pesan.tools.servicespy.action.RxHttpAdapter.rxCompletable;
import static org.github.pesan.tools.servicespy.action.RxHttpAdapter.rxMaybe;
import static org.github.pesan.tools.servicespy.action.RxHttpAdapter.rxServerSentEvents;
import static org.github.pesan.tools.servicespy.action.RxHttpAdapter.rxSingle;

public class DashboardVerticle extends AbstractVerticle {

    private final Map<RequestId, Buffer> requestData = new ConcurrentHashMap<>();
    private final Map<RequestId, Buffer> responseData = new ConcurrentHashMap<>();

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
                .handler(rxServerSentEvents(request -> actionService.streamList().map(Json::fromLogEntry).map(JsonObject::encode)));
        trafficRouter.get("/:id/data/request/")
                .handler(rxMaybe(request ->
                {
                    RequestId requestId = RequestId.fromText(request.getParam("id"));
                    return Maybe.zip(
                            actionService.byId(requestId).map(LogEntry::getRequest),
                            Maybe.fromCallable(() -> requestData.get(requestId)),
                            (entry, data) -> RxHttpAdapter.HttpResponse.ok(data)
                                    .withHeaders(entry.getContentType() != null ? Map.of("Content-Type", entry.getContentType()) : Map.of())
                    );
                }));
        trafficRouter.get("/:id/data/response/")
                .handler(rxMaybe(request ->
                {
                    RequestId requestId = RequestId.fromText(request.getParam("id"));
                    return Maybe.zip(
                            actionService.byId(requestId).map(LogEntry::getResponse),
                            Maybe.fromCallable(() -> responseData.get(requestId)),
                            (entry, data) -> RxHttpAdapter.HttpResponse.ok(data)
                                    .withHeaders(entry.getContentType() != null ? Map.of("Content-Type", entry.getContentType()) : Map.of())
                    );
                }));

        AtomicReference<RequestDataEntry> last = new AtomicReference<>();

        vertx.eventBus().<JsonObject>consumer("request.begin", message -> {
            JsonObject body = message.body();
            if (!last.compareAndSet(null, Json.toRequestDataEntry(body.getJsonObject("requestData")))) {
                throw new RuntimeException("out of order");
            }
        });
        vertx.eventBus().<JsonObject>consumer("response.begin", message -> {
            JsonObject body = message.body();
            RequestDataEntry requestDataEntry = last.getAndSet(null);
            if (requestDataEntry == null) {
                throw new NullPointerException();
            }
            ResponseDataEntry responseDataEntry = Json.toResponseDataEntry(body.getJsonObject("responseData"));
            actionService.log(RequestId.fromText(body.getString("requestId")), requestDataEntry, responseDataEntry);

        });

        Function<Map<RequestId, Buffer>, Handler<Message<JsonObject>>> handle = storage -> msg -> {
            RequestId requestId = RequestId.fromText(msg.body().getString("requestId"));
            byte[] payload = msg.body().getBinary("payload");
            storage.compute(requestId, (__, buffer) -> buffer == null
                    ? Buffer.buffer(payload)
                    : buffer.appendBytes(payload));
        };

        vertx.eventBus().consumer("request.data", handle.apply(requestData));
        vertx.eventBus().consumer("response.data", handle.apply(responseData));

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