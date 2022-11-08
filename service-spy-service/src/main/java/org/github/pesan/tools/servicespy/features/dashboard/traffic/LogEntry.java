package org.github.pesan.tools.servicespy.features.dashboard.traffic;

import org.github.pesan.tools.servicespy.application.event.RequestDataEntry;
import org.github.pesan.tools.servicespy.application.RequestId;
import org.github.pesan.tools.servicespy.application.event.ResponseDataEntry;

import java.time.ZoneOffset;
import java.util.Optional;

public record LogEntry(RequestId id,
                       RequestDataEntry request,
                       Optional<ResponseDataEntry> response) {

    public long getResponseTimeMillis() {
        // TODO:
        return response().map(response ->
                        response.time().toInstant(ZoneOffset.UTC).toEpochMilli()
                                - request().time().toInstant(ZoneOffset.UTC).toEpochMilli())
                .orElse(0L);
    }

}