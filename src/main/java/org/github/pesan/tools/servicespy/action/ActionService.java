package org.github.pesan.tools.servicespy.action;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import io.vertx.core.buffer.Buffer;
import org.github.pesan.tools.servicespy.action.entry.LogEntry;
import org.github.pesan.tools.servicespy.action.entry.RequestDataEntry;
import org.github.pesan.tools.servicespy.action.entry.ResponseDataEntry;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ActionService {

    private final int maxEntryCount;
    private final LinkedHashMap<RequestId, LogEntry> logEntries = new LinkedHashMap<>();

    private final Map<RequestId, Buffer> requestData = new HashMap<>();
    private final Map<RequestId, Buffer> responseData = new HashMap<>();

    private Subject<LogEntry> publisher;

    public ActionService(int maxEntryCount) {
        this.maxEntryCount = maxEntryCount;
        init();
    }

    private void init() {
        publisher = PublishSubject.<LogEntry>create().toSerialized();
    }

    public Observable<LogEntry> list() {
        return Observable.fromIterable(logEntries.values());
    }

    public Maybe<LogEntry> byId(RequestId id) {
        return list()
                .filter(e -> e.getId().equals(id))
                .singleElement();
    }

    public Observable<LogEntry> subscribe() {
        return list().concatWith(publisher);
    }

    public Completable clear() {
        return Completable.fromAction(logEntries::clear);
    }

    public void onBeginRequest(RequestId requestId, RequestDataEntry requestData) {
        logEntries.put(requestId, new LogEntry(requestId, requestData, null));
    }

    public void onResponseBegin(RequestId requestId, ResponseDataEntry responseEntry) {
        logEntries.compute(requestId, (__, logEntry) -> new LogEntry(
                requestId,
                logEntry != null ? logEntry.getRequest() : null,
                responseEntry
        ));
    }

    public void onRequestData(RequestId requestId, byte[] payload) {
        requestData.compute(requestId, (__, buffer) -> buffer == null ? Buffer.buffer(payload) : buffer.appendBytes(payload));
    }

    public void onResponseData(RequestId requestId, byte[] payload) {
        responseData.compute(requestId, (__, buffer) -> buffer == null ? Buffer.buffer(payload) : buffer.appendBytes(payload));
    }

    public void onEnd(RequestId requestId) {
        if (logEntries.containsKey(requestId)) {
            publisher.onNext(logEntries.get(requestId));
        }
    }

    public Maybe<Buffer> getRequestData(RequestId requestId) {
        return Maybe.fromCallable(() -> responseData.get(requestId));
    }

    public Maybe<Buffer> getResponseData(RequestId requestId) {
        return Maybe.fromCallable(() -> responseData.get(requestId));
    }
}