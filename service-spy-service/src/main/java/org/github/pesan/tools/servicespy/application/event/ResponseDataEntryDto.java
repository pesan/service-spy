package org.github.pesan.tools.servicespy.application.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.github.pesan.platform.web.HttpHeaders;
import org.github.pesan.tools.servicespy.application.ExceptionDetails;
import org.github.pesan.tools.servicespy.features.dashboard.traffic.IsoDateDto;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record ResponseDataEntryDto(
        @JsonProperty("statusCode") int statusCode,
        @JsonProperty("url") URL url,
        @JsonProperty("headers") Map<String, List<String>> headers,
        @JsonProperty("contentType") String contentType,
        @JsonProperty("data") byte[] data,
        @JsonProperty("time") IsoDateDto time,
        @JsonProperty("exception") Optional<ExceptionDetails> exception) {

    // TODO:
    public String getHost() {
        URL url = url();
        if (url == null) return "";
        return url.getHost() + (url.getPort() != -1 ? ":" + url.getPort() : url.getDefaultPort() != -1 ? ":" + url.getDefaultPort() : "");
    }

    // TODO:
    public String getHostName() {
        URL url = url();
        if (url == null) return "";
        return url.getHost();
    }

    // TODO:
    public int getPort() {
        URL url = url();
        if (url == null) return 0;
        return url.getPort() != -1 ? url.getPort() : url.getDefaultPort() != -1 ? url.getDefaultPort() : 0;
    }

    // TODO:
    ResponseDataEntryDto fail(ExceptionDetails exceptionDetails) {
        return null;
        //return new ResponseDataEntry(status, contentType, url, headers, responseData, time, exceptionDetails);
    }

    public static ResponseDataEntryDto fromModel(ResponseDataEntry response) {
        return new ResponseDataEntryDto(
                response.status(),
                response.url(),
                response.headers().asMap(),
                response.contentType(),
                response.data(),
                IsoDateDto.fromLocalDateTime(response.time()),
                response.exception()
        );
    }

    public static ResponseDataEntry toModel(ResponseDataEntryDto responseDataEntry) {
        return new ResponseDataEntry(
                responseDataEntry.statusCode(),
                responseDataEntry.url(),
                HttpHeaders.fromMap(responseDataEntry.headers()),
                responseDataEntry.contentType(),
                responseDataEntry.data(),
                responseDataEntry.time().time(),
                responseDataEntry.exception()
        );
    }
}