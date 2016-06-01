package org.github.pesan.tools.servicespy.action;

import org.github.pesan.tools.servicespy.action.entry.LogEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
}

