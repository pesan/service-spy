package org.github.pesan.tools.servicespy.action;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.subjects.ReplaySubject;
import io.reactivex.subjects.Subject;
import org.github.pesan.tools.servicespy.action.entry.LogEntry;
import org.github.pesan.tools.servicespy.action.entry.RequestEntry;
import org.github.pesan.tools.servicespy.action.entry.ResponseEntry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class ActionService {

    private final RequestIdGenerator requestIdGenerator;
    private final int maxEntryCount;

    private Subject<LogEntry> buffer;
    private ReplaySubject<LogEntry> replay;

    public ActionService(
            RequestIdGenerator requestIdGenerator,
            @Value("${actions.limit:200}") int maxEntryCount) {
        this.requestIdGenerator = requestIdGenerator;
        this.maxEntryCount = maxEntryCount;
        init();
    }

    private void init() {
        replay = ReplaySubject.createWithSize(maxEntryCount);
        buffer = replay.toSerialized();
    }

    public Observable<LogEntry> list() {
        return replay.take(1, TimeUnit.MILLISECONDS);
    }

    public Observable<LogEntry> streamList() {
        return replay;
    }

    public void log(RequestEntry requestEntry, ResponseEntry responseEntry) {
        buffer.onNext(new LogEntry(requestIdGenerator.next(), requestEntry, responseEntry));
    }

    public Completable clear() {
        return Completable.fromAction(this::init);
    }
}
