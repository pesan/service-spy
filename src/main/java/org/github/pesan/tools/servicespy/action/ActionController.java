package org.github.pesan.tools.servicespy.action;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.github.pesan.tools.servicespy.action.entry.LogEntry;
import org.github.pesan.tools.servicespy.action.entry.RequestDataEntry;
import org.github.pesan.tools.servicespy.action.entry.ResponseDataEntry;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
@RequestMapping("/api/actions")
public class ActionController {
    private final ActionService actionService;

    public ActionController(ActionService actionService) {
        this.actionService = actionService;
    }

    @RequestMapping(method=DELETE)
    @ResponseStatus(NO_CONTENT)
    public Completable reset() {
        return actionService.clear();
    }

    @RequestMapping(method=GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Single<List<LogEntry>> list() {
        return actionService.list().toList();
    }

    @RequestMapping(method=GET, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Observable<LogEntry>> streamList() {
        return ResponseEntity.ok()
                             .header(HttpHeaders.CACHE_CONTROL, "no-transform") // To work with node proxy
                             .body(actionService.streamList());
    }

    @RequestMapping(value="/{id}/data/request/", method=GET)
    public Maybe<ResponseEntity<byte[]>> requestData(@PathVariable("id") String id) {
        return actionService.list()
                .filter(entry -> entry.getId().equals(id))
                .singleElement()
                .map(LogEntry::getRequest)
                .filter(RequestDataEntry.class::isInstance)
                .cast(RequestDataEntry.class)
                .map(entry -> new ResponseEntity<>(entry.getData(), contentType(entry.getContentType()), OK));
    }

    @RequestMapping(value="/{id}/data/response/", method=GET)
    public Maybe<ResponseEntity<byte[]>> responseData(@PathVariable("id") String id) {
        return actionService.list()
                .filter(entry -> entry.getId().equals(id))
                .singleElement()
                .map(LogEntry::getResponse)
                .filter(ResponseDataEntry.class::isInstance)
                .cast(ResponseDataEntry.class)
                .map(entry -> new ResponseEntity<>(entry.getData(), contentType(entry.getContentType()), OK));
    }

    private HttpHeaders contentType(String contentType) {
        HttpHeaders headers = new HttpHeaders();
        if (contentType != null) {
            headers.add(HttpHeaders.CONTENT_TYPE, contentType);
        }
        return headers;
    }
}

