package org.github.pesan.platform.web;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

public class HttpHeaders {

    private static final HttpHeaders EMPTY = new HttpHeaders(Map.of());

    private final Map<String, List<String>> headers;

    private HttpHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
    }

    public static HttpHeaders empty() {
        return EMPTY;
    }

    public static HttpHeaders fromHeader(String key, String value) {
        return fromUniMap(Map.of(key, value));
    }

    public static HttpHeaders fromUniMap(Map<String, String> headers) {
        return new HttpHeaders(headers.entrySet().stream().collect(toMap(e -> e.getKey().toLowerCase(), e -> List.of(e.getValue()))));
    }

    public static HttpHeaders fromMap(Map<String, ? extends Collection<String>> headers) {
        return new HttpHeaders(headers.entrySet().stream().collect(toMap(e -> e.getKey().toLowerCase(), e -> List.copyOf(e.getValue()))));
    }

    public Optional<List<String>> getHeader(String key) {
        return Optional.ofNullable(headers.get(key.toLowerCase()));
    }

    public Optional<String> getFirstHeader(String key) {
        return getHeader(key)
                .flatMap(values -> values.stream().findFirst());
    }

    public Stream<Map.Entry<String, List<String>>> stream() {
        return headers.entrySet().stream();
    }

    public Map<String, List<String>> asMap() {
        return headers;
    }

    public boolean has(String key) {
        return headers.containsKey(key.toLowerCase());
    }

    public HttpHeaders withoutKey(String key) {
        return has(key)
                ? HttpHeaders.fromMap(stream()
                .filter(entry -> !entry.getKey().equalsIgnoreCase(key))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                : this;
    }
}