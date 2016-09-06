package org.github.pesan.tools.servicespy.proxy;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.github.pesan.tools.servicespy.action.ActionService;
import org.github.pesan.tools.servicespy.action.entry.NoMappingException;
import org.github.pesan.tools.servicespy.action.entry.RequestDataEntry;
import org.github.pesan.tools.servicespy.action.entry.ResponseDataEntry;
import org.github.pesan.tools.servicespy.action.entry.ResponseEntry;
import org.github.pesan.tools.servicespy.action.entry.ResponseExceptionEntry;
import org.github.pesan.tools.servicespy.proxy.HttpServerBindings.Binding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProxyService extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(ProxyService.class);

    private @Autowired Vertx vertx;

    private @Autowired ActionService actionService;
    private @Autowired ProxyProperties proxyProperties;

    private @Autowired HttpClientBindings proxyClients;
    private @Autowired HttpServerBindings proxyServers;

    private @Autowired Clock clock;

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
            .requestHandler(this::handleRequest)
            .listen(listenHandlerForServer(server));
    }

    private void handleRequest(HttpServerRequest serverRequest) {
        ByteArrayOutputStream received = new ByteArrayOutputStream();
        ByteArrayOutputStream sent = new ByteArrayOutputStream();

        RequestContext context = new RequestContext(serverRequest, getClockTime());

        try {
            URL backendUrl = getBackendUrl(context);
            HttpClientRequest clientRequest = createClientRequest(serverRequest, backendUrl)
                    .handler(responseHandler(context, sent, received, backendUrl))
                    .exceptionHandler(responseExceptionHandler(context, backendUrl, sent, received))
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

    private URL getBackendUrl(RequestContext context) {
        return createURL(proxyProperties.getMappings().stream()
                .filter(ProxyProperties.Mapping::isActive)
                .filter(mapping -> mapping.getPattern().matcher(context.getRequestPath()).matches())
                .map(mapping -> mapping.getUrl() + context.getRequestPathWithQuery())
                .findFirst()
                .orElseThrow(() -> new NoMappingException(context.getRequestPath())));
    }

    private Handler<Throwable> responseExceptionHandler(RequestContext context, URL backendUrl, ByteArrayOutputStream sent, ByteArrayOutputStream received) {
        return throwable -> {
            actionService.log(
                    RequestDataEntry.fromContext(context, sent),
                    new ResponseExceptionEntry(backendUrl, throwable, getClockTime())
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
                        new ResponseExceptionEntry(backendUrl, throwable, getClockTime()));
                serverResponse.close();
            }).endHandler(v -> {
                actionService.log(
                        RequestDataEntry.fromContext(context, sent),
                        new ResponseDataEntry(clientResponse.statusCode(), clientResponse.getHeader(HttpHeaders.CONTENT_TYPE), backendUrl, parseHeaders(clientResponse.headers()), received.toByteArray(), getClockTime()));
                serverResponse.end();
            });
         };
    }

    static Map<String, List<String>> parseHeaders(MultiMap headers) {
        return headers.entries().stream().collect(
                groupingBy(Map.Entry::getKey, mapping(Map.Entry::getValue, toList())));
    }

    private Handler<AsyncResult<HttpServer>> listenHandlerForServer(Binding server) {
        return result -> {
            String name = server.getName();
            String host = server.getHost();
            int port = server.getPort();
            if (result.succeeded()) {
                logger.info("Proxy server '{}' listening on {}:{}", name, host, port);
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
