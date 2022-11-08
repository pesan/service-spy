package org.github.pesan.tools.servicespy.application.event;

import org.github.pesan.platform.web.HttpHeaders;
import org.github.pesan.tools.servicespy.application.ExceptionDetails;

import java.time.LocalDateTime;
import java.util.Optional;

public record RequestDataEntry(String requestPath,
                               Optional<String> query,
                               String httpMethod,
                               HttpHeaders headers,
                               LocalDateTime time,
                               byte[] data,
                               String contentType,
                               Optional<ExceptionDetails> exception) {

    public RequestDataEntry fail(ExceptionDetails details) {
        return new RequestDataEntry(requestPath, query, httpMethod, headers, time, data, contentType, Optional.of(details));
    }
}