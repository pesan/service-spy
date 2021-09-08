package org.github.pesan.tools.servicespy.dashboard.traffic;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import io.vertx.core.buffer.Buffer;
import org.github.pesan.tools.servicespy.dashboard.HttpHeaders;
import org.github.pesan.tools.servicespy.dashboard.model.RequestId;
import org.github.pesan.tools.servicespy.dashboard.model.ExceptionDetails;
import org.github.pesan.tools.servicespy.dashboard.model.LogEntry;
import org.github.pesan.tools.servicespy.dashboard.model.RequestDataEntry;
import org.github.pesan.tools.servicespy.dashboard.model.ResponseDataEntry;

import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

class ActionService {

    private final Map<RequestId, LogEntry> logEntries;

    private final Map<RequestId, Buffer> requestData = new HashMap<>();
    private final Map<RequestId, Buffer> responseData = new HashMap<>();

    private final Subject<LogEntry> publisher = PublishSubject.<LogEntry>create().toSerialized();

    ActionService(int maxEntryCount) {
        this.logEntries = createFIFO(maxEntryCount, removedRequestId -> {
            requestData.remove(removedRequestId);
            responseData.remove(removedRequestId);
        });
    }

    Observable<LogEntry> list() {
        return Observable.fromIterable(logEntries.values());
    }

    Maybe<LogEntry> byId(RequestId id) {
        return list()
                .filter(e -> e.getId().equals(id))
                .singleElement();
    }

    Observable<LogEntry> subscribe() {
        return list().concatWith(publisher);
    }

    Completable clear() {
        return Completable.fromAction(logEntries::clear);
    }

    void onBeginRequest(RequestId requestId, RequestDataEntry requestData) {
        logEntries.put(requestId, new LogEntry(requestId, requestData, null));
    }

    void onResponseBegin(RequestId requestId, ResponseDataEntry responseEntry) {
        logEntries.compute(requestId, (__, logEntry) -> new LogEntry(
                requestId,
                logEntry != null ? logEntry.getRequest() : null,
                responseEntry
        ));
    }

    void onRequestData(RequestId requestId, byte[] payload) {
        requestData.compute(requestId, (__, buffer) -> buffer == null ? Buffer.buffer(payload) : buffer.appendBytes(payload));
    }

    void onResponseData(RequestId requestId, byte[] payload) {
        responseData.compute(requestId, (__, buffer) -> buffer == null ? Buffer.buffer(payload) : buffer.appendBytes(payload));
    }

    void onResponseError(RequestId requestId, ExceptionDetails exceptionDetails) {
        logEntries.compute(requestId, (__, logEntry) -> new LogEntry(
                requestId,
                logEntry != null ? logEntry.getRequest() : new RequestDataEntry(
                        URI.create("/"), "", HttpHeaders.empty(), new ByteArrayOutputStream(), LocalDateTime.now(), null
                ),
                logEntry != null && logEntry.getResponse() != null ? logEntry.getResponse().fail(exceptionDetails) : new ResponseDataEntry(
                        0, "", dummy(), HttpHeaders.empty(), new byte[0], LocalDateTime.now()
                ).fail(exceptionDetails)
        ));
        onEnd(requestId);
    }

    private URL dummy() {
        try {
            return new URL("http://localhost");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    void onEnd(RequestId requestId) {
        if (logEntries.containsKey(requestId)) {
            publisher.onNext(logEntries.get(requestId));
        }
    }

    Maybe<Content> getRequestData(RequestId requestId) {
        return Maybe.fromCallable(() -> new Content(
                Optional.ofNullable(logEntries.get(requestId)).map(e -> e.getRequest().getContentType()).orElse(null),
                requestData.get(requestId)
        ));
    }

    Maybe<Content> getResponseData(RequestId requestId) {
        return Maybe.fromCallable(() -> new Content(
                Optional.ofNullable(logEntries.get(requestId)).map(e -> e.getResponse().getContentType()).orElse(null),
                responseData.get(requestId)
        ));
    }

    static class Content {
        private final String contentType;
        private final Buffer data;

        public Content(String contentType, Buffer data) {
            this.contentType = contentType;
            this.data = data;
        }

        public String getContentType() {
            return contentType;
        }

        public Buffer getData() {
            return data;
        }
    }

    private <K, V> Map<K, V> createFIFO(final int maxEntries, Consumer<K> onRemove) {
        return new LinkedHashMap<>(maxEntries * 10 / 7, 0.7f, false) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                if (size() > maxEntries) {
                    onRemove.accept(eldest.getKey());
                    return true;
                }
                return false;
            }
        };
    }
}