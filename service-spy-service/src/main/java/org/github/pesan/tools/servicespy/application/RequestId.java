package org.github.pesan.tools.servicespy.application;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.UUID;

public record RequestId(UUID uuid) {
    @JsonCreator
    public static RequestId fromText(String id) {
        return new RequestId(UUID.fromString(id));
    }

    public static RequestId random() {
        return new RequestId(UUID.randomUUID());
    }

    @JsonValue
    public String toText() {
        return uuid().toString();
    }
}