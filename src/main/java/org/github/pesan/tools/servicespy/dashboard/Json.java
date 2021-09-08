package org.github.pesan.tools.servicespy.dashboard;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.github.pesan.tools.servicespy.dashboard.entry.ExceptionDetails;
import org.github.pesan.tools.servicespy.dashboard.entry.ExceptionDetails.StackFrame;
import org.github.pesan.tools.servicespy.dashboard.entry.LogEntry;
import org.github.pesan.tools.servicespy.dashboard.entry.RequestDataEntry;
import org.github.pesan.tools.servicespy.dashboard.entry.ResponseDataEntry;
import org.github.pesan.tools.servicespy.dashboard.config.ProxyServer;
import org.github.pesan.tools.servicespy.proxy.ProxyProperties;

import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class Json {

    public static JsonObject fromLogEntry(LogEntry logEntry) {
        return new JsonObject()
                .put("id", logEntry.getId().toText())
                .put("responseTimeMillis", logEntry.getResponseTimeMillis())
                .put("href", new JsonObject()
                        .put("requestData", String.format("/api/traffic/%s/data/request/", logEntry.getId()))
                        .put("responseData", String.format("/api/traffic/%s/data/response/", logEntry.getId()))
                )
                .put("request", fromRequestDataEntry(logEntry.getRequest()))
                .put("response", fromResponseDataEntry(logEntry.getResponse()));
    }

    public static RequestDataEntry toRequestDataEntry(JsonObject json) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.writeBytes(json.getBinary("data"));
        return new RequestDataEntry(
                URI.create(json.getString("requestPath") + json.getString("query", "")),
                json.getString("httpMethod"),
                toHttpHeaders(json.getJsonObject("headers")),
                outputStream,
                LocalDateTime.parse(json.getString("time"), DateTimeFormatter.ISO_DATE_TIME),
                null // TODO:
        );
    }

    public static JsonObject fromRequestDataEntry(RequestDataEntry requestEntry) {
        return new JsonObject()
                .put("requestPath", requestEntry.getRequestPath())
                .put("headers", new JsonObject(requestEntry.getHeaders().stream()
                        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue))))
                .put("httpMethod", requestEntry.getHttpMethod())
                .put("contentType", requestEntry.getContentType())
                .put("query", requestEntry.getQuery())
                .put("data", requestEntry.getData())
                .put("time", requestEntry.getTime().format(DateTimeFormatter.ISO_DATE_TIME))
                .put("exception", requestEntry.getException() != null ? fromExceptionDetails(requestEntry.getException()) : null);
    }

    public static ResponseDataEntry toResponseDataEntry(JsonObject json) {
        return new ResponseDataEntry(
                json.getInteger("status"),
                json.getString("contentType"),
                toURL(json.getString("url")).orElseThrow(),
                toHttpHeaders(json.getJsonObject("headers")),
                json.getBinary("data"),
                LocalDateTime.parse(json.getString("time"), DateTimeFormatter.ISO_DATE_TIME)
        );
    }

    public static JsonObject fromResponseDataEntry(ResponseDataEntry responseEntry) {
        return new JsonObject()
                .put("status", responseEntry.getStatus())
                .put("contentType", responseEntry.getContentType())
                .put("url", responseEntry.getUrl().toString())
                .put("headers", fromHttpHeaders(responseEntry.getHeaders()))
                .put("data", responseEntry.getData())
                .put("time", responseEntry.getTime().format(DateTimeFormatter.ISO_DATE_TIME))
                .put("host", responseEntry.getHost())
                .put("hostName", responseEntry.getHostName())
                .put("port", responseEntry.getPort())
                .put("exception", responseEntry.getException() != null ? fromExceptionDetails(responseEntry.getException()) : null);
    }

    public static ProxyProperties toProxyProperties(JsonObject json) {
        return new ProxyProperties(
                json.getInteger("connectionTimeout", 5000),
                json.getInteger("idleTimeout", 5000),
                objectStream(json.getJsonObject("servers"))
                        .map(e -> Map.entry(e.getKey(), toProxyServer(e.getValue())))
                        .collect(toList())
        );
    }

    public static JsonObject fromProxyProperties(ProxyProperties properties) {
        return new JsonObject()
                .put("connectionTimeout", properties.getConnectionTimeout())
                .put("idleTimeout", properties.getIdleTimeout())
                .put("servers", new JsonObject(properties.getServers().entrySet().stream()
                        .collect(toMap(
                                Map.Entry::getKey,
                                e -> fromProxyServer(e.getValue()))))
                );
    }

    private static ProxyServer toProxyServer(JsonObject json) {
        return new ProxyServer(
                json.getString("host", "0.0.0.0"),
                json.getInteger("port"),
                json.getBoolean("ssl", false),
                new ProxyServer.KeystoreConfig(
                        json.getString("jksKeystore"),
                        json.getString("jksPassword"),
                        json.getString("pfxKeystore"),
                        json.getString("pfxPassword"),
                        json.getString("pemKeystore"),
                        json.getString("pemPassword")
                ),
                objectStream(json.getJsonArray("mappings"))
                        .map(Json::toProxyMapping)
                        .collect(toList())
        );
    }

    private static JsonObject fromProxyServer(ProxyServer value) {
        return new JsonObject()
                .put("host", value.getHost())
                .put("port", value.getPort())
                .put("ssl", value.getSsl())
                .put("jksKeystore", value.getKeystoreConfig().getJksKeystore())
                .put("jksPassword", value.getKeystoreConfig().getJksPassword())
                .put("pfxKeystore", value.getKeystoreConfig().getPfxKeystore())
                .put("pfxPassword", value.getKeystoreConfig().getPfxPassword())
                .put("pemKeystore", value.getKeystoreConfig().getPemCertPath())
                .put("pemPassword", value.getKeystoreConfig().getPemKeyPath())
                .put("mappings", new JsonArray(value.getMappings().stream()
                        .map(Json::fromProxyMapping)
                        .collect(toList())
                ));
    }

    private static ProxyServer.Mapping toProxyMapping(JsonObject json) {
        return new ProxyServer.Mapping(
                Pattern.compile(json.getString("pattern", "/*")),
                toURL(json.getString("url", "")).orElseThrow(),
                json.getBoolean("active", true)
        );
    }

    private static JsonObject fromProxyMapping(ProxyServer.Mapping m) {
        JsonObject json = new JsonObject();
        json.put("active", m.isActive());
        json.put("url", m.getUrl().toString());
        json.put("pattern", m.getPattern().toString());
        return json;
    }

    private static Stream<JsonObject> objectStream(JsonArray json) {
        return IntStream.range(0, json.size())
                .mapToObj(json::getJsonObject);
    }

    private static Stream<Map.Entry<String, JsonObject>> objectStream(JsonObject json) {
        return json.fieldNames()
                .stream()
                .map(field -> Map.entry(field, json.getJsonObject(field)));
    }

    private static JsonObject fromHttpHeaders(HttpHeaders headers) {
        return new JsonObject(headers.stream().collect(toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    @SuppressWarnings("unchecked")
    private static HttpHeaders toHttpHeaders(JsonObject json) {
        return HttpHeaders.fromMap(json.stream()
                .collect(toMap(Map.Entry::getKey, e -> (List<String>) ((JsonArray) e.getValue()).getList())));
    }

    private static Optional<URL> toURL(String url) {
        try {
            return Optional.of(new URL(url));
        } catch (MalformedURLException e) {
            return Optional.empty();
        }
    }

    public static Settings toSettings(JsonObject config) {
        return new Settings(
                config.getInteger("port"),
                config.getString("webroot")
        );
    }

    public static JsonObject fromExceptionDetails(ExceptionDetails exceptionDetails) {
        return new JsonObject()
                .put("name", exceptionDetails.getName())
                .put("message", exceptionDetails.getMessage())
                .put("stackTrace", new JsonArray(exceptionDetails.getStackTrace().stream()
                        .map(Json::fromStackFrame)
                        .collect(toList())));

    }

    public static ExceptionDetails toExceptionDetails(JsonObject json) {
        return new ExceptionDetails(
                json.getString("name"),
                json.getString("message"),
                ((List<JsonObject>)json.getJsonArray("stackTrace").getList())
                        .stream()
                        .map(Json::toStackFrame).collect(toList())
        );
    }

    public static JsonObject fromStackFrame(StackFrame frame) {
        return new JsonObject()
                .put("lineNumber", frame.getLineNumber())
                .put("className", frame.getClassName())
                .put("methodName", frame.getMethodName())
                .put("fileName", frame.getFileName());
    }

    public static StackFrame toStackFrame(JsonObject json) {
        return new StackFrame(
                json.getInteger("lineNumber"),
                json.getString("className"),
                json.getString("methodName"),
                json.getString("fileName")
        );
    }
}