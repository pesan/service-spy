package org.github.pesan.tools.servicespy.action;

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

    public RequestLogEntry beginRequest(String requestId, String requestPath, String requestPathWithQuery, String httpMethod) {
        return new RequestLogEntry(requestId, requestPath, requestPathWithQuery, httpMethod);
    }

    public void endRequest(RequestLogEntry entry, int status, String contentType, URL url, String requestData, String responseData) {
        buffer.onNext(entry.endRequest(status, contentType, url, requestData, responseData));
    }

    public void endRequest(RequestLogEntry entry, Throwable throwable) {
        buffer.onNext(entry.endRequest(throwable));
    }

    public void endRequest(RequestLogEntry entry, URL url, Throwable throwable) {
        buffer.onNext(entry.endRequest(throwable, url));
    }

    public Observable<RequestLogEntry> list() {
        return Observable.from(replay.getValues(new RequestLogEntry[replay.size()]));
    }

    public Observable<RequestLogEntry> streamList() {
        return replay;
    }
}
