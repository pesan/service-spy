package org.github.pesan.tools.servicespy.features.dashboard.traffic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static java.util.Objects.requireNonNull;

public record IsoDateDto(LocalDateTime time) {
    @JsonCreator
    public static IsoDateDto fromText(String text) {
        return new IsoDateDto(LocalDateTime.parse(text, DateTimeFormatter.ISO_DATE_TIME));
    }

    public static IsoDateDto fromLocalDateTime(LocalDateTime time) {
        return new IsoDateDto(requireNonNull(time));
    }

    @JsonValue
    public String toText() {
        return time.format(DateTimeFormatter.ISO_DATE_TIME);
    }
}
