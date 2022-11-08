package org.github.pesan.tools.servicespy.features.dashboard.traffic;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import org.github.pesan.tools.servicespy.application.ExceptionDetails;
import org.github.pesan.tools.servicespy.application.RequestId;
import org.github.pesan.tools.servicespy.application.event.RequestDataEntry;
import org.github.pesan.tools.servicespy.application.event.ResponseDataEntry;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

class ActionService {

    private final Map<RequestId, LogEntry> logEntries;

    private final Map<RequestId, List<byte[]>> requestData = new HashMap<>();
    private final Map<RequestId, List<byte[]>> responseData = new HashMap<>();

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

    Observable<LogEntry> subscribe() {
        return list().concatWith(publisher);
    }

    void onRequestBegin(RequestId requestId, RequestDataEntry requestData) {
        logEntries.put(requestId, new LogEntry(requestId, requestData, Optional.empty()));
    }

    void onResponseBegin(RequestId requestId, ResponseDataEntry responseEntry) {
        logEntries.compute(requestId, (__, logEntry) ->
                new LogEntry(requestId, logEntry != null
                        ? logEntry.request()
                        : null,
                        Optional.of(responseEntry)));
    }

    void onRequestData(RequestId requestId, byte[] payload) {
        requestData.compute(requestId, (__, buffer) -> buffer == null
                ? List.of(payload)
                : Stream.concat( buffer.stream(), Stream.of(payload)).toList()); // TODO;
    }

    void onResponseData(RequestId requestId, byte[] payload) {
        responseData.compute(requestId, (__, buffer) -> buffer == null
                ? List.of(payload)
                : Stream.concat( buffer.stream(), Stream.of(payload)).toList()); // TODO;
    }

    void onResponseError(RequestId requestId, ExceptionDetails exceptionDetails) {
        logEntries.compute(requestId, (__, logEntry) -> new LogEntry(requestId,
                logEntry.request(),
                logEntry.response().map(n -> n.fail(exceptionDetails))
        ));
        onEnd(requestId);
    }

    private static URL exampleComUrl() {
        try {
            return new URL("http://www.example.com");
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
        return Maybe.fromOptional(Optional.ofNullable(requestData.get(requestId))
                .map(data -> new Content(
                        logEntries.get(requestId).request().contentType(),
                        data
                )));
    }

    Maybe<Content> getResponseData(RequestId requestId) {
        return Maybe.fromOptional(Optional.ofNullable(responseData.get(requestId))
                .map(data -> new Content(
                        logEntries.get(requestId).response().map(ResponseDataEntry::contentType).orElse("application/octet-stream"),
                        data
                )));
    }

    static class Content {
        private final String contentType;
        private final List<byte[]> data;

        public Content(String contentType, List<byte[]> data) {
            this.contentType = contentType;
            this.data = data;
        }

        public String getContentType() {
            return contentType;
        }

        public Flowable<byte[]> getData() {
            return Flowable.fromIterable(data);
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