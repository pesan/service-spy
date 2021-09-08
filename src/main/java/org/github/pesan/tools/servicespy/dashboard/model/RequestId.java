package org.github.pesan.tools.servicespy.dashboard.model;

import java.util.Objects;
import java.util.UUID;

public class RequestId {

    private final UUID id;

    private RequestId(UUID id) {
        this.id = id;
    }

    public static RequestId fromText(String text) {
        return new RequestId(UUID.fromString(text));
    }

    public String toText() {
        return id.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RequestId requestId = (RequestId) o;
        return Objects.equals(id, requestId.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return toText();
    }
}