package org.github.pesan.tools.servicespy.proxy;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import org.github.pesan.tools.servicespy.action.ActionService;
import org.github.pesan.tools.servicespy.action.entry.NoMappingException;
import org.github.pesan.tools.servicespy.action.entry.RequestDataEntry;
import org.github.pesan.tools.servicespy.action.entry.ResponseDataEntry;
import org.github.pesan.tools.servicespy.action.entry.ResponseEntry;
import org.github.pesan.tools.servicespy.action.entry.ResponseExceptionEntry;
import org.github.pesan.tools.servicespy.config.ProxyServer;
import org.github.pesan.tools.servicespy.proxy.HttpServerBindings.Binding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.github.pesan.tools.servicespy.action.entry.ExceptionDetails.fromThrowable;

@Component
public class ProxyService extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(ProxyService.class);

    private final Vertx vertx;
    private final ActionService actionService;
    private final HttpClientBindings proxyClients;
    private final HttpServerBindings proxyServers;
    private final Clock clock;

    public ProxyService(Vertx vertx, ActionService actionService, HttpClientBindings proxyClients, HttpServerBindings proxyServers, Clock clock) {
        this.vertx = vertx;
        this.actionService = actionService;
        this.proxyClients = proxyClients;
        this.proxyServers = proxyServers;
        this.clock = clock;
    }

    @PostConstruct
    public void init() {
        vertx.deployVerticle(this);
    }

    @Override
    public void start() {
        proxyServers.stream().forEach(this::startServer);
    }

    private void startServer(Binding server) {
        server.getServer()
            .requestHandler(serverRequest -> handleRequest(server.getMappings(), serverRequest))
            .listen(listenHandlerForServer(server));
    }

    private void handleRequest(List<ProxyServer.Mapping> mappings, HttpServerRequest serverRequest) {
        ByteArrayOutputStream received = new ByteArrayOutputStream();
        ByteArrayOutputStream sent = new ByteArrayOutputStream();

        RequestContext context = new RequestContext(serverRequest, getClockTime());

        try {
            URL backendUrl = getBackendUrl(mappings, context);
            HttpClientRequest clientRequest = createClientRequest(serverRequest, backendUrl)
                    .handler(responseHandler(context, sent, received, backendUrl))
                    .exceptionHandler(responseExceptionHandler(context, backendUrl, sent))
                    .setChunked(true);

            clientRequest.headers().setAll(serverRequest.headers());
            serverRequest.handler(data -> {
                write(sent, data);
                clientRequest.write(data);
            });
            serverRequest.endHandler(v -> clientRequest.end());

        } catch (NoMappingException e) {
            logger.warn(e.getMessage(), e);
            serverRequest.response().close();
            actionService.log(
                    RequestDataEntry.fromContext(context, e),
                    ResponseEntry.empty(null, getClockTime()));
        }
    }

    private HttpClientRequest createClientRequest(HttpServerRequest serverRequest, URL backendUrl) {
        return proxyClients.getByScheme(backendUrl.getProtocol())
                .request(serverRequest.method(), getPort(backendUrl), backendUrl.getHost(), serverRequest.uri());
    }

    private int getPort(URL backendUrl) {
        int port = backendUrl.getPort();
        return port != -1 ? port : backendUrl.getDefaultPort();
    }

    private URL getBackendUrl(List<ProxyServer.Mapping> mappings, RequestContext context) {
        return createURL(mappings.stream()
                .filter(ProxyServer.Mapping::isActive)
                .filter(mapping -> mapping.getPattern().matcher(context.getRequestPath()).matches())
                .map(mapping -> mapping.getUrl() + context.getRequestPathWithQuery())
                .findFirst()
                .orElseThrow(() -> new NoMappingException(context.getRequestPath())));
    }

    private Handler<Throwable> responseExceptionHandler(RequestContext context, URL backendUrl, ByteArrayOutputStream sent) {
        return throwable -> {
            actionService.log(
                    RequestDataEntry.fromContext(context, sent),
                    new ResponseExceptionEntry(backendUrl, fromThrowable(throwable), getClockTime())
            );
            context.getResponse().close();
         };
    }

    private Handler<HttpClientResponse> responseHandler(RequestContext context, ByteArrayOutputStream sent, ByteArrayOutputStream received, URL backendUrl) {
        return clientResponse -> {
            HttpServerResponse serverResponse = context.getResponse()
                    .setChunked(true)
                    .setStatusCode(clientResponse.statusCode());
            serverResponse.headers().setAll(clientResponse.headers());
            clientResponse.handler(data -> {
                write(received, data);
                serverResponse.write(data);
            }).exceptionHandler(throwable -> {
                actionService.log(
                        RequestDataEntry.fromContext(context, sent),
                        new ResponseExceptionEntry(backendUrl, fromThrowable(throwable), getClockTime()));
                serverResponse.close();
            }).endHandler(v -> {
                actionService.log(
                        RequestDataEntry.fromContext(context, sent),
                        new ResponseDataEntry(clientResponse.statusCode(), clientResponse.getHeader(HttpHeaders.CONTENT_TYPE), backendUrl, parseHeaders(clientResponse.headers()), received.toByteArray(), getClockTime()));
                serverResponse.end();
            });
         };
    }

    private static Map<String, List<String>> parseHeaders(MultiMap headers) {
        return headers.entries().stream().collect(
                groupingBy(Map.Entry::getKey, mapping(Map.Entry::getValue, toList())));
    }

    private Handler<AsyncResult<HttpServer>> listenHandlerForServer(Binding server) {
        return result -> {
            String name = server.getName();
            String host = server.getHost();
            int port = server.getPort();
            if (result.succeeded()) {
                logger.info("Proxy server '{}' listening on {}:{} with {} mapping(s)", name, host, port, server.getMappings().size());
            } else {
                logger.error("Unable to start '{}' proxy server on {}:{}", name, host, port, result.cause());
            }
        };
    }

    private LocalDateTime getClockTime() {
        return LocalDateTime.ofInstant(clock.instant(), clock.getZone());
    }

    private void write(OutputStream stream, Buffer buffer) {
        try {
            stream.write(buffer.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private URL createURL(String backendUrl) {
        try {
            return new URL(backendUrl);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
