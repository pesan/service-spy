package org.github.pesan.tools.servicespy.action;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.reactivex.rxjava3.subjects.Subject;
import org.github.pesan.tools.servicespy.action.entry.LogEntry;
import org.github.pesan.tools.servicespy.action.entry.RequestDataEntry;
import org.github.pesan.tools.servicespy.action.entry.ResponseDataEntry;

import java.util.concurrent.atomic.AtomicInteger;

public class ActionService {

    private final AtomicInteger count = new AtomicInteger();
    private final int maxEntryCount;

    private Subject<LogEntry> buffer;

    public ActionService(int maxEntryCount) {
        this.maxEntryCount = maxEntryCount;
        init();
    }

    private void init() {
        count.set(0);
        buffer = ReplaySubject.<LogEntry>createWithSize(maxEntryCount).toSerialized();
    }

    public Observable<LogEntry> list() {
        return buffer.take(count.get());
    }

    public Maybe<LogEntry> byId(RequestId id) {
        return list()
                .filter(e -> e.getId().equals(id))
                .singleElement();
    }

    public Observable<LogEntry> streamList() {
        return buffer;
    }

    public void log(RequestId requestId, RequestDataEntry requestEntry, ResponseDataEntry responseEntry) {
        buffer.onNext(new LogEntry(requestId, requestEntry, responseEntry));
        count.getAndUpdate(current -> Math.min(current + 1, maxEntryCount));
    }

    public Completable clear() {
        return Completable.fromAction(this::init);
    }
}