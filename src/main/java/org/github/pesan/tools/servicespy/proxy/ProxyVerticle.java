package org.github.pesan.tools.servicespy.proxy;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.httpproxy.ProxyRequest;
import org.github.pesan.tools.servicespy.dashboard.HttpHeaders;
import org.github.pesan.tools.servicespy.dashboard.Json;
import org.github.pesan.tools.servicespy.dashboard.model.RequestId;
import org.github.pesan.tools.servicespy.dashboard.model.ExceptionDetails;
import org.github.pesan.tools.servicespy.dashboard.model.RequestDataEntry;
import org.github.pesan.tools.servicespy.dashboard.model.ResponseDataEntry;
import org.github.pesan.tools.servicespy.dashboard.config.ProxyServer;

import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;

public class ProxyVerticle extends AbstractVerticle {
    private List<HttpServer> httpServers = List.of();
    private final ProxyConfig proxyConfig = new ProxyConfig();

    @Override
    public void start(Promise<Void> startup) throws Exception {
        ProxyProperties initialProxyProperties = Json.toProxyProperties(config());

        HttpClient httpClient = vertx.createHttpClient(proxyConfig.httpClientOptions(initialProxyProperties));
        HttpClient httpsClient = vertx.createHttpClient(proxyConfig.httpsClientOptions(initialProxyProperties));

        reloadServers(httpClient, httpsClient, initialProxyProperties)
                .onSuccess(__ -> vertx.eventBus().<JsonObject>consumer("proxy.settings", message -> {
                    ProxyProperties proxyProperties = Json.toProxyProperties(message.body());

                    System.out.println("Reloading: " + message.body().encodePrettily());

                    reloadServers(httpClient, httpsClient, proxyProperties)
                            .onSuccess(startedServers -> {
                                message.reply("proxy.reloaded");
                            })
                            .onFailure(e -> {
                                e.printStackTrace();
                                message.fail(1, e.getMessage());
                            });
                }))
                .onComplete(startup);
    }

    @SuppressWarnings("unchecked")
    private Future<Void> reloadServers(HttpClient httpClient, HttpClient httpsClient, ProxyProperties proxyProperties) {
        return CompositeFuture
                .all(httpServers.stream().map(HttpServer::close).collect(toList()))
                .compose(__ -> CompositeFuture.all(proxyProperties.getServers()
                        .entrySet()
                        .stream()
                        .map(proxyServerEntry -> {
                            ProxyServer proxyServer = proxyServerEntry.getValue();
                            HttpServer httpServer = vertx.createHttpServer(proxyConfig.createServerOptions(proxyServer));
                            return httpServer.requestHandler(request -> {
                                        String path = request.path();
                                        proxyServer.getMappings().stream()
                                                .filter(mapping -> mapping.isActive() && mapping.getPattern().matcher(path).matches())
                                                .findFirst()
                                                .ifPresentOrElse(mapping -> doProxy(mapping, request, httpClient, httpsClient),
                                                        () -> request.response().setStatusCode(502).end());
                                    })
                                    .listen(proxyServer.getPort())
                                    .map(server -> Map.entry(proxyServerEntry.getKey(), server));
                        }).collect(toList())))
                .onSuccess(startedServers -> {
                    httpServers = startedServers.list().stream()
                            .map(e -> (Map.Entry<String, HttpServer>) e)
                            .map(Map.Entry::getValue).collect(toList());

                    startedServers.list().stream()
                            .map(serverEntry -> (Map.Entry<String, HttpServer>) serverEntry)
                            .map(serverEntry -> new JsonObject().put(serverEntry.getKey(), serverEntry.getValue().actualPort()))
                            .reduce(JsonObject::mergeIn)
                            .ifPresent(portsConfig ->
                                    vertx.eventBus().publish("proxy.started", new JsonObject()
                                            .put("ports", portsConfig)
                                    ));
                })
                .onFailure(Throwable::printStackTrace)
                .mapEmpty();
    }

    private void doProxy(ProxyServer.Mapping mapping, HttpServerRequest request, HttpClient httpClient, HttpClient httpsClient) {
        EventBus eventBus = vertx.eventBus();
        ProxyRequest proxyRequest = ProxyRequest.reverseProxy(request);

        RequestId requestId = requestId();

        int port = mapping.getUrl().getPort() >= 0 ? mapping.getUrl().getPort() : mapping.getUrl().getDefaultPort();
        String host = mapping.getUrl().getHost();
        HttpClient client = mapping.getUrl().getProtocol().equals("https") ? httpsClient : httpClient;

        client.request(request.method(), port, host, request.uri())
                .compose(httpClientRequest -> {
                    String httpMethod = request.method().toString();
                    URI requestUri = URI.create(request.uri());
                    MultiMap headersMultiMap = request.headers();
                    HttpHeaders headers = HttpHeaders.fromMultiMap(headersMultiMap);
                    eventBus.publish("request.begin", requestBeginEvent(requestId, requestUri, httpMethod, headers));
                    request.bodyHandler(buff -> eventBus.publish("request.data", requestDataEvent(requestId, buff)));
                    request.endHandler(__ -> eventBus.publish("request.end", requestEndEvent(requestId)));

                    return proxyRequest.send(httpClientRequest)
                            .onComplete(result -> {
                                eventBus.publish("request.end", responseEndEvent(requestId));
                            });
                })
                .onSuccess(proxyResponse -> {
                    HttpHeaders headers = HttpHeaders.fromMultiMap(proxyResponse.headers());
                    int status = proxyResponse.getStatusCode();

                    String contentType = headers.getFirstHeader("content-type").orElse(null);
                    URL url = null;
                    try {
                        url = new URL("http://do-not-know.example.com");
                    } catch (MalformedURLException ignored) {
                    }

                    eventBus.publish("response.begin", responseBeginEvent(requestId, status, contentType, url, headers));
                    eventBus.publish("response.data", responseDataEvent(requestId, Buffer.buffer("not-implemented".getBytes(UTF_8))));

                    proxyResponse.send()
                            .onComplete(result -> {
                                eventBus.publish("response.end", responseEndEvent(requestId));
                            });
                })
                .onFailure(err -> {
                    // Release the request
                    eventBus.publish("response.error", responseFailedEvent(requestId, err));

                    proxyRequest.release();

                    // Send error
                    request.response().setStatusCode(502)
                            .send();
                });
    }

    private JsonObject requestEndEvent(RequestId requestId) {
        return new JsonObject()
                .put("requestId", requestId.toText());
    }

    private JsonObject responseEndEvent(RequestId requestId) {
        return new JsonObject()
                .put("requestId", requestId.toText());
    }

    private JsonObject requestDataEvent(RequestId requestId, Buffer chunk) {
        return new JsonObject()
                .put("requestId", requestId.toText())
                .put("payload", chunk.getBytes());

    }

    private JsonObject responseDataEvent(RequestId requestId, Buffer chunk) {
        return new JsonObject()
                .put("requestId", requestId.toText())
                .put("payload", chunk.getBytes());
    }

    private JsonObject requestBeginEvent(RequestId requestId, URI requestUri, String httpMethod, HttpHeaders headers) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.writeBytes("click download".getBytes(UTF_8));
        JsonObject requestData = Json.fromRequestDataEntry(new RequestDataEntry(
                requestUri,
                httpMethod,
                headers,
                outputStream,
                LocalDateTime.now(),
                null
        ));
        JsonObject event = new JsonObject();
        event.put("requestId", requestId.toText());
        event.put("payload", requestData);
        return event;
    }

    private JsonObject responseBeginEvent(RequestId requestId, int status, String contentType, URL url, HttpHeaders headers) {
        JsonObject responseData = Json.fromResponseDataEntry(new ResponseDataEntry(
                status,
                contentType,
                url,
                headers,
                "click download".getBytes(UTF_8),
                LocalDateTime.now()
        ));
        JsonObject event = new JsonObject();
        event.put("requestId", requestId.toText());
        event.put("payload", responseData);
        return event;
    }

    private JsonObject responseFailedEvent(RequestId requestId, Throwable exception) {
        JsonObject responseFailed = Json.fromExceptionDetails(ExceptionDetails.fromThrowable(exception));
        return new JsonObject()
                .put("requestId", requestId.toText())
                .put("payload", responseFailed);
    }

    private RequestId requestId() {
        return RequestId.fromText(UUID.randomUUID().toString());
    }
}