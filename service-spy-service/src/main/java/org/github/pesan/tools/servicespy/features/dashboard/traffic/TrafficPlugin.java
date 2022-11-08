package org.github.pesan.tools.servicespy.features.dashboard.traffic;

import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Single;
import org.github.pesan.platform.Platform;
import org.github.pesan.platform.messaging.MessageBus;
import org.github.pesan.platform.web.Route;
import org.github.pesan.platform.web.Routes;
import org.github.pesan.platform.web.WebRequest;
import org.github.pesan.platform.web.WebResponse;
import org.github.pesan.tools.servicespy.application.RequestId;
import org.github.pesan.tools.servicespy.application.event.RequestDataEntryDto;
import org.github.pesan.tools.servicespy.application.event.ResponseDataEntryDto;
import org.github.pesan.tools.servicespy.application.event.TEvent;

import java.util.Map;

import static org.github.pesan.platform.web.HttpHeaders.fromHeader;

public class TrafficPlugin {
    private final ActionService actionService;

    private final Route requestDataRoute = new Route("GET", "/:id/data/request", this::handleDownloadRequestDataById);
    private final Route responseDataRoute = new Route("GET", "/:id/data/response", this::handleDownloadResponseDataById);

    public TrafficPlugin(Platform platform) {
        this.actionService = new ActionService(100); // TODO: from config

        MessageBus messageBus = platform.messageBus();

        messageBus.consume(TEvent.RequestBegin.class, "request.begin", msg ->
                actionService.onRequestBegin(msg.id(), RequestDataEntryDto.toModel(msg.payload())));

        messageBus.consume(TEvent.RequestData.class, "request.data", msg ->
                actionService.onRequestData(msg.id(), msg.payload()));

        messageBus.consume(TEvent.ResponseBegin.class, "response.begin", msg ->
                actionService.onResponseBegin(msg.id(), ResponseDataEntryDto.toModel(msg.payload())));

        messageBus.consume(TEvent.ResponseData.class, "response.data", msg ->
                actionService.onResponseData(msg.id(), msg.payload()));

        messageBus.consume(TEvent.RequestEnd.class, "request.end", msg -> {});
        messageBus.consume(TEvent.ResponseEnd.class, "response.end", msg ->
                actionService.onEnd(msg.id()));

        messageBus.consume(TEvent.ResponseError.class, "response.error", msg ->
                actionService.onResponseError(msg.id(), msg.payload()));
    }

    public Single<Routes> router() {
        Routes routes = Routes.of(
                Routes.GET("", this::handleGetTrafficEvents)
                        .config(route -> route.produces("application/json")),
                Routes.GET("", this::handleStreamTrafficEvents)
                        .config(route -> route.produces("text/event-stream")),
                Routes.of(requestDataRoute),
                Routes.of(responseDataRoute)
        );

        return Single.just(routes);
    }

    private Single<WebResponse> handleGetTrafficEvents(WebRequest request) {
        return actionService.list()
                .map(this::toDto)
                .toList()
                .map(events -> WebResponse.json(Map.of("events", events)));

    }

    private Single<WebResponse> handleStreamTrafficEvents(WebRequest request) {
        return Single.just(WebResponse.eventStream(
                actionService.subscribe().toFlowable(BackpressureStrategy.BUFFER)
                        .map(this::toDto)
        ));
    }

    private LogEntryDto toDto(LogEntry logEntry) {
        return LogEntryDto.fromModel(logEntry,
                "/api/traffic" + requestDataRoute.build(Map.of(
                        "id", logEntry.id().toText()
                )),
                "/api/traffic" + responseDataRoute.build(Map.of(
                        "id", logEntry.id().toText()
                )));
    }

    private Single<WebResponse> handleDownloadRequestDataById(WebRequest request) {
        Map<String, String> pathParameters = request.matchedPath().parse(request.uri());
        return actionService.getRequestData(RequestId.fromText(pathParameters.get("id")))
                .map(content -> new WebResponse(
                        200,
                        fromHeader("Content-Type", content.getContentType()),
                        content.getData(),
                        true))
                .defaultIfEmpty(WebResponse.notFound());
    }

    private Single<WebResponse> handleDownloadResponseDataById(WebRequest request) {
        Map<String, String> pathParameters = request.matchedPath().parse(request.uri());
        return actionService.getResponseData(RequestId.fromText(pathParameters.get("id")))
                .map(content -> new WebResponse(
                        200,
                        fromHeader("Content-Type", content.getContentType()),
                        content.getData(),
                        true))
                .defaultIfEmpty(WebResponse.notFound());
    }
}