package org.github.pesan.tools.servicespy.common;

import org.github.pesan.tools.servicespy.admin.RequestLogEntry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rx.Observable;
import rx.subjects.ReplaySubject;
import rx.subjects.SerializedSubject;
import rx.subjects.Subject;

import javax.annotation.PostConstruct;
import java.net.URL;

@Service
public class ActionService {
    @Value("${actions.limit:200}")
    private int maxEntryCount;

    private Subject<RequestLogEntry, RequestLogEntry> buffer;
    private ReplaySubject<RequestLogEntry> replay = ReplaySubject.<RequestLogEntry>createWithSize(maxEntryCount);

    @PostConstruct
    public void init() {
        replay = ReplaySubject.<RequestLogEntry>createWithSize(maxEntryCount);
        buffer = new SerializedSubject<>(replay);
    }

    public RequestLogEntry beginRequest(String requestId, String service, URL url) {
        return new RequestLogEntry(requestId, service, url);
    }

    public void endRequest(RequestLogEntry entry, String requestDataIn, String requestDataOut, String responseDataIn, String responseDataOut) {
        buffer.onNext(entry.endRequest(requestDataIn, requestDataOut, responseDataIn, responseDataOut));
    }

    public void endRequest(RequestLogEntry entry, Exception e) {
        buffer.onNext(entry.endRequest(e));
    }

    public Observable<RequestLogEntry> list() {
        return Observable.from(replay.getValues(new RequestLogEntry[replay.size()]));
    }

    public Observable<RequestLogEntry> streamList() {
        return replay;
    }

    public Observable<Boolean> deleteAll() {
        return Observable
                .just(true)
                .doOnNext(x -> init());
    }
}
