package org.github.pesan.tools.servicespy.proxy;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
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

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.github.pesan.tools.servicespy.action.ActionService;
import org.github.pesan.tools.servicespy.action.entry.RequestEntry;
import org.github.pesan.tools.servicespy.action.entry.ResponseDataEntry;
import org.github.pesan.tools.servicespy.action.entry.ResponseExceptionEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProxyService extends AbstractVerticle {
    private static final Log logger = LogFactory.getLog(ProxyService.class);

    private @Autowired ActionService actionService;
    private @Autowired ProxyProperties proxyProperties;

    private @Autowired HttpClientBindings proxyClients;
    private @Autowired HttpServerBindings proxyServers;

    private @Autowired Clock clock;

    @PostConstruct
    public void init() {
        Vertx.vertx().deployVerticle(this);
    }

    @Override
    public void start() {
        proxyServers.stream().forEach(server -> {
            startServer(server.getName(), server.getServer(), server.getPort());
        });
    }

    private void startServer(String name, HttpServer httpServer, int port) {
        httpServer.requestHandler(this::handleRequest).listen(port, result -> {
            if (result.succeeded()) {
                logger.info(String.format("'%s' proxy server started on port %d", name, port));
            } else {
                logger.fatal(String.format("Unable to start '%s' proxy server on port %d", name, port), result.cause());
            }
        });
    }

    private void handleRequest(HttpServerRequest serverRequest) {
        String requestPath = serverRequest.path();
        String requestPathWithQuery = serverRequest.uri();
        ByteArrayOutputStream received = new ByteArrayOutputStream();
        ByteArrayOutputStream sent = new ByteArrayOutputStream();

        RequestEntry reqEntry = new RequestEntry(requestPath, requestPathWithQuery, serverRequest.method().name(), sent, getClockTime());
        try {
            URL backendUrl = createURL(proxyProperties.getMappings().stream()
                    .filter(ProxyProperties.Mapping::isActive)
                    .filter(mapping -> mapping.getPattern().matcher(requestPath).matches())
                    .map(mapping -> mapping.getUrl() + requestPathWithQuery)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No mapping for request path: " + requestPath)));

            HttpClient client = proxyClients.getByScheme(backendUrl.getProtocol());
            HttpClientRequest clientRequest = client.request(serverRequest.method(), backendUrl.getPort(), backendUrl.getHost(), serverRequest.uri(), clientResponse -> {
                HttpServerResponse serverResponse = serverRequest.response()
                        .setChunked(true)
                        .setStatusCode(clientResponse.statusCode());
                serverResponse.headers().setAll(clientResponse.headers());
                clientResponse.handler(data -> {
                    write(received, data);
                    serverResponse.write(data);
                }).exceptionHandler(throwable -> {
                    actionService.log(reqEntry, new ResponseExceptionEntry(backendUrl, throwable, getClockTime()));
                    serverResponse.close();
                }).endHandler(v -> {
                    actionService.log(reqEntry, new ResponseDataEntry(clientResponse.statusCode(), clientResponse.getHeader("Content-Type"), backendUrl, received.toString(), getClockTime()));
                    serverResponse.end();
                });
            }).exceptionHandler(throwable -> {
                actionService.log(reqEntry, new ResponseExceptionEntry(backendUrl, throwable, getClockTime()));
                serverRequest.response().close();
            });
            clientRequest.setChunked(true);
            clientRequest.headers().setAll(serverRequest.headers());
            serverRequest.handler(data -> {
                write(sent, data);
                clientRequest.write(data);
            });
            serverRequest.endHandler(v -> clientRequest.end());

        } catch (RuntimeException e) {
            logger.warn(e.getMessage(), e);
            serverRequest.response().close();
            actionService.log(reqEntry, new ResponseExceptionEntry(null, e, getClockTime()));

        }
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
