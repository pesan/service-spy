package org.github.pesan.tools.servicespy.application.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.github.pesan.platform.web.HttpHeaders;
import org.github.pesan.tools.servicespy.application.ExceptionDetails;
import org.github.pesan.tools.servicespy.features.dashboard.traffic.IsoDateDto;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record RequestDataEntryDto(
        @JsonProperty("requestPath") String requestPath,
        @JsonProperty("query") Optional<String> query,
        @JsonProperty("httpMethod") String httpMethod,
        @JsonProperty("headers") Map<String, List<String>> headers,
        @JsonProperty("time") IsoDateDto time,
        @JsonProperty("data") byte[] data,
        @JsonProperty("contentType") String contentType,
        @JsonProperty("exception") Optional<ExceptionDetails> exception) {

    public static RequestDataEntry toModel(RequestDataEntryDto requestDataEntryDto) {
        return new RequestDataEntry(
                requestDataEntryDto.requestPath(),
                requestDataEntryDto.query(),
                requestDataEntryDto.httpMethod(),
                HttpHeaders.fromMap(requestDataEntryDto.headers()),
                requestDataEntryDto.time().time(),
                requestDataEntryDto.data(),
                requestDataEntryDto.contentType(),
                requestDataEntryDto.exception()
        );
    }

    public static RequestDataEntryDto fromModel(RequestDataEntry requestDataEntry) {
        return new RequestDataEntryDto(
                requestDataEntry.requestPath(),
                requestDataEntry.query(),
                requestDataEntry.httpMethod(),
                requestDataEntry.headers().asMap(),
                IsoDateDto.fromLocalDateTime(requestDataEntry.time()),
                requestDataEntry.data(),
                requestDataEntry.contentType(),
                requestDataEntry.exception()
        );
    }
}