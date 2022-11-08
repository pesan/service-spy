package org.github.pesan.platform.web;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toMap;

public class RequestPath {

    private static final RequestPath EMPTY = new RequestPath(new String[0]);
    private final String[] components;

    private RequestPath(String[] components) {
        this.components = components;
    }

    public static RequestPath fromString(String text) {
        return RequestPath.fromComponents(
                sanitize(text.split("/+"))
        );
    }

    public static RequestPath empty() {
        return EMPTY;
    }

    private static String[] sanitize(String[] components) {
        return Arrays.stream(components)
                .filter(component -> component != null && !component.isBlank())
                .map(component -> URLDecoder.decode(component, UTF_8))
                .toArray(String[]::new);
    }

    public static RequestPath fromComponents(String... components) {
        return components.length == 0 ? EMPTY : new RequestPath(components);
    }

    public Map<String, String> parse(String path) {
        RequestPath requestPath = RequestPath.fromString(path);
        return IntStream.range(0, requestPath.components.length)
                .filter(index -> components[index].startsWith(":"))
                .boxed()
                .collect(toMap(
                        index -> components[index].substring(1),
                        index -> requestPath.components[index]
                ));
    }

    public  Stream<String> components() {
        return Arrays.stream(components);
    }

    public RequestPath concat(RequestPath requestPath) {
        return isEmpty() ? requestPath : requestPath.isEmpty() ? this :
                new RequestPath(join(this.components, requestPath.components));
    }

    private static String[] join(String[] a, String[] b) {
        String[] result = new String[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    public boolean isEmpty() {
        return this.components.length == 0;
    }

    public String asPath() {
        return asPath(__ -> "");
    }

    public String asPath(Map<String, String> variables) {
        return asPath(variables::get);
    }

    public String asPath(UnaryOperator<String> lookup) {
        return "/" + asRelativePath(lookup);
    }

    public String asRelativePath() {
        return asRelativePath(__ -> "");
    }

    public String asRelativePath(Map<String, String> variables) {
        return asRelativePath(variables::get);
    }

    public String asRelativePath(UnaryOperator<String> variables) {
        return components()
                .map(component -> component.startsWith(":") ? variables.apply(component.substring(1)) : component)
                .map(component -> URLEncoder.encode(component, UTF_8))
                .collect(Collectors.joining("/"));
    }

    public String asRaw() {
        return "/" + String.join("/", components);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RequestPath that = (RequestPath) o;
        return Arrays.equals(components, that.components);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(components);
    }

    public RequestPath prefix(RequestPath prefix) {
        return new RequestPath(join(prefix.components, components));
    }
}
