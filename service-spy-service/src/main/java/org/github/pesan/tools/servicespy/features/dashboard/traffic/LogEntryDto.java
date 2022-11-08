package org.github.pesan.tools.servicespy.features.dashboard.traffic;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.github.pesan.tools.servicespy.application.event.RequestDataEntryDto;
import org.github.pesan.tools.servicespy.application.RequestId;
import org.github.pesan.tools.servicespy.application.event.ResponseDataEntryDto;

import java.time.ZoneOffset;
import java.util.Map;

public record LogEntryDto(
    @JsonProperty("id") RequestId id,
    @JsonProperty("request") RequestDataEntryDto request,
    @JsonProperty("response") ResponseDataEntryDto response,
    @JsonProperty("href") Map<String, String> href) {

    public long getResponseTimeMillis() {
        // TODO:
        if (response != null) {
            return response.time().time().toInstant(ZoneOffset.UTC).toEpochMilli()
                    - request.time().time().toInstant(ZoneOffset.UTC).toEpochMilli();
        }
        return 0;
    }

    public static LogEntryDto fromModel(LogEntry e, String requestDataUri, String responseDataUri) {
        return new LogEntryDto(
                e.id(),
                RequestDataEntryDto.fromModel(e.request()),
                e.response().map(ResponseDataEntryDto::fromModel).orElse(null),
                Map.of(
                        "requestData", requestDataUri,
                        "responseData", responseDataUri
                ));
    }
}