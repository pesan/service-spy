package org.github.pesan.tools.servicespy.action;

import io.vertx.core.MultiMap;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class HttpHeaders {

    private final Map<String, List<String>> headers;

    private HttpHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
    }

    public static HttpHeaders fromUniMap(Map<String, String> headers) {
        return new HttpHeaders(headers.entrySet().stream().collect(toMap(e -> e.getKey().toLowerCase(), e -> List.of(e.getValue()))));
    }

    public static HttpHeaders fromMap(Map<String, ? extends Collection<String>> headers) {
        return new HttpHeaders(headers.entrySet().stream().collect(toMap(e -> e.getKey().toLowerCase(), e -> List.copyOf(e.getValue()))));
    }

    public static HttpHeaders fromMultiMap(MultiMap headers) {
        return new HttpHeaders(headers.entries()
                .stream()
                .collect(groupingBy(
                        e -> e.getKey().toLowerCase(),
                        Collectors.mapping(Map.Entry::getValue, toList()))
                ));
    }

    public boolean hasHeader(String key) {
        return headers.containsKey(key.toLowerCase());
    }

    public Optional<List<String>> getHeader(String key) {
        return Optional.ofNullable(headers.get(key.toLowerCase()));
    }

    public Optional<String> getFirstHeader(String key) {
        return getHeader(key)
                .flatMap(l -> l.stream().findFirst());
    }

    public Optional<String> getSingleHeader(String key) {
        return getHeader(key).flatMap(l -> {
            if (l.size() <= 1) {
                throw new IllegalStateException("expected single value for header " + key + ", was " + l.size());
            }
            return l.stream().findFirst();
        });
    }

    public Stream<Map.Entry<String, List<String>>> stream() {
        return headers.entrySet().stream();
    }

    public Map<String, List<String>> asMap() {
        return headers;
    }
}