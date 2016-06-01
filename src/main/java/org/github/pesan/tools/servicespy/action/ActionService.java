package org.github.pesan.tools.servicespy.action;

import org.github.pesan.tools.servicespy.action.entry.LogEntry;
import org.github.pesan.tools.servicespy.action.entry.RequestEntry;
import org.github.pesan.tools.servicespy.action.entry.ResponseEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rx.Observable;
import rx.subjects.ReplaySubject;
import rx.subjects.SerializedSubject;
import rx.subjects.Subject;

import javax.annotation.PostConstruct;

@Service
public class ActionService {
    @Value("${actions.limit:200}")
    private int maxEntryCount;

    private @Autowired RequestIdGenerator requestIdGenerator;

    private Subject<LogEntry, LogEntry> buffer;
    private ReplaySubject<LogEntry> replay = ReplaySubject.createWithSize(maxEntryCount);

    @PostConstruct
    public void init() {
        replay = ReplaySubject.createWithSize(maxEntryCount);
        buffer = new SerializedSubject<>(replay);
    }

    public Observable<LogEntry> list() {
        return Observable.from(replay.getValues(new LogEntry[replay.size()]));
    }

    public Observable<LogEntry> streamList() {
        return replay;
    }

    public void log(RequestEntry requestEntry, ResponseEntry responseEntry) {
        buffer.onNext(new LogEntry(requestIdGenerator.next(), requestEntry, responseEntry));
    }

    public Observable<Boolean> clear() {
        return Observable.just(true)
                .doOnCompleted(this::init);
    }
}
