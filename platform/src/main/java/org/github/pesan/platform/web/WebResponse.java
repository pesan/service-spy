package org.github.pesan.platform.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableTransformer;

import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;

public class WebResponse {

    private static final ObjectMapper RESPONSE_MAPPER = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final HttpHeaders STREAM_HEADERS = HttpHeaders.fromUniMap(Map.of(
            "Content-Type", "text/event-stream",
            "Connection", "keep-alive",
            "Cache-Control", "no-transform", // no-cache?
            "Access-Control-Allow-Origin", "*"
    ));

    private final int status;
    private final HttpHeaders headers;
    private final Flowable<byte[]> body;
    private final boolean chunked;

    public WebResponse(int status, HttpHeaders headers, Flowable<byte[]> body, boolean chunked) {
        this.status = status;
        this.headers = headers;
        this.body = body;
        this.chunked = chunked;
    }

    public static WebResponse ok() {
        return new WebResponse(204, HttpHeaders.empty(), Flowable.empty(), false);
    }

    public static WebResponse ok(String contentType, byte[] payload) {
        return withPayload(200, contentType, payload);
    }

    public static <T> WebResponse json(T jsonPayload) {
        try {
            return ok("application/json", RESPONSE_MAPPER.writeValueAsBytes(jsonPayload));
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static WebResponse withPayload(int status, String contentType, byte[] payload) {
        return new WebResponse(
                status,
                HttpHeaders.fromUniMap(Map.of(
                        "Content-Length", String.valueOf(payload.length),
                        "Content-Type", contentType
                )),
                Flowable.just(payload),
                false
        );
    }

    public static WebResponse eventStream(Flowable<? extends Object> events) {
        Flowable<byte[]> eventStream = Flowable.merge(
                dataEventStream(events),
                pingEventStream(events)
        );

        return new WebResponse(200, STREAM_HEADERS, eventStream, true);
    }

    private static Flowable<byte[]> dataEventStream(Flowable<?> events) {
        return events.map(event -> eventPayload("data", RESPONSE_MAPPER.writeValueAsString(event)));
    }

    private static Flowable<byte[]> pingEventStream(Flowable<?> events) {
        return Flowable.interval(1, TimeUnit.MINUTES)
                .map(__ -> eventPayload("event", "ping"))
                .takeUntil(events.ignoreElements().toFlowable());
    }

    private static byte[] eventPayload(String type, String body) {
        return (type + ": " + body + "\n\n").getBytes(UTF_8);
    }

    public static WebResponse forStatus(int status) {
        return new WebResponse(status, HttpHeaders.fromUniMap(Map.of("Content-Length", "0")), Flowable.empty(), false);
    }

    public static WebResponse badRequest() {
        return forStatus(400);
    }

    public static WebResponse notFound() {
        return forStatus(404);
    }

    public int status() {
        return status;
    }

    public HttpHeaders headers() {
        return headers;
    }

    public Flowable<byte[]> body() {
        return body;
    }

    public boolean isChunked() {
        return chunked;
    }

    public WebResponse compose(FlowableTransformer<? super byte[], ? extends byte[]> composer) {
        return new WebResponse(
                status,
                headers.withoutKey("content-length"),
                body.compose(composer),
                true
        );
    }
}
