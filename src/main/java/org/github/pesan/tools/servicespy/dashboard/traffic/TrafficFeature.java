package org.github.pesan.tools.servicespy.dashboard.traffic;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import org.github.pesan.tools.servicespy.dashboard.Json;
import org.github.pesan.tools.servicespy.dashboard.model.RequestId;
import org.github.pesan.tools.servicespy.http.HttpResponse;

import java.util.Map;

import static org.github.pesan.tools.servicespy.http.RxHttpAdapter.rxCompletable;
import static org.github.pesan.tools.servicespy.http.RxHttpAdapter.rxMaybe;
import static org.github.pesan.tools.servicespy.http.RxHttpAdapter.rxServerSentEvents;
import static org.github.pesan.tools.servicespy.http.RxHttpAdapter.rxSingle;

public class TrafficFeature {
    private final Vertx vertx;
    private final JsonObject config;

    public TrafficFeature(Vertx vertx, JsonObject config) {
        this.vertx = vertx;
        this.config = config;
    }

    public Future<Router> router() {
        Router trafficRouter = Router.router(vertx);

        int maxEntryCount = config.getJsonObject("actions", new JsonObject()).getInteger("limit", 200);
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
                    RequestId requestId = RequestId.fromText(request.requireParam("id"));
                    return actionService.getRequestData(requestId)
                            .map(content -> HttpResponse.ok(content.getData())
                                    .withHeaders(content.getContentType() != null ? Map.of("Content-Type", content.getContentType()) : Map.of())
                            );
                }));
        trafficRouter.get("/:id/data/response/")
                .handler(rxMaybe(request -> {
                    RequestId requestId = RequestId.fromText(request.requireParam("id"));
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

}