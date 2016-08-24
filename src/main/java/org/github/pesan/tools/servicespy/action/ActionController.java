package org.github.pesan.tools.servicespy.action;

import org.github.pesan.tools.servicespy.action.entry.LogEntry;
import org.github.pesan.tools.servicespy.action.entry.RequestEntry;
import org.github.pesan.tools.servicespy.action.entry.ResponseDataEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import rx.Observable;

import java.io.IOException;
import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
@RequestMapping("/api/actions")
public class ActionController {
    private @Autowired ActionService actionService;
    private @Value("${stream.timeout:86400000}") long timeout;

    @RequestMapping(method=GET)
    public Observable<List<LogEntry>> list() {
        return actionService.list().toList();
    }

    @RequestMapping(method=DELETE)
    public Observable<HttpStatus> reset() {
        return actionService.clear().map(x -> HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value="/{id}/data/request/", method=GET)
    public Observable<byte[]> requestData(@PathVariable("id") String id) {
        return actionService.streamList()
                .filter(entry -> entry.getId().equals(id))
                .map(LogEntry::getRequest)
                .map(RequestEntry::getData);
    }

    @RequestMapping(value="/{id}/data/response/", method=GET)
    public Observable<ResponseEntity<byte[]>> responseData(@PathVariable("id") String id) {
        return actionService.streamList()
                .filter(entry -> entry.getId().equals(id))
                .map(LogEntry::getResponse)
                .filter(ResponseDataEntry.class::isInstance)
                .cast(ResponseDataEntry.class)
                .map(entry -> new ResponseEntity<>(entry.getData(), contentType(entry.getContentType()), HttpStatus.OK));
    }

    @RequestMapping(value="/stream", method=GET, produces="text/event-stream")
    public SseEmitter streamList() throws IOException {
        SseEmitter sseEmitter = new SseEmitter(timeout);
        actionService.streamList()
                .subscribe(
                        logEntry -> {
                            try {
                                sseEmitter.send(logEntry, MediaType.APPLICATION_JSON);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        },
                        sseEmitter::completeWithError,
                        sseEmitter::complete
                );
        return sseEmitter;
    }

    private HttpHeaders contentType(String contentType) {
        HttpHeaders headers = new HttpHeaders();
        if (contentType != null) {
            headers.add(HttpHeaders.CONTENT_TYPE, contentType);
        }
        return headers;
    }
}

