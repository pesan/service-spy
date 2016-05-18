package org.github.pesan.tools.servicespy.proxy;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.github.pesan.tools.servicespy.action.ActionService;
import org.github.pesan.tools.servicespy.action.RequestLogEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

@Component
public class ProxyService extends AbstractVerticle {
    private static final Log logger = LogFactory.getLog(ProxyService.class);

    private @Autowired ActionService actionService;
    private @Autowired ProxyProperties proxyProperties;

    private @Autowired HttpClient httpClient;
    private @Autowired HttpServer httpServer;

    @Override
    public void start() {
        httpServer.requestHandler(serverRequest -> {
            String requestPath = serverRequest.path();
            String requestPathWithQuery = serverRequest.uri();
            ByteArrayOutputStream received = new ByteArrayOutputStream();
            ByteArrayOutputStream sent = new ByteArrayOutputStream();

            RequestLogEntry entry = actionService.beginRequest(UUID.randomUUID().toString(), requestPath, requestPathWithQuery, serverRequest.method().name());
            try {
                URL backendUrl = createURL(proxyProperties.getMappings().stream()
                        .filter(ProxyProperties.Mapping::isActive)
                        .filter(mapping -> mapping.getPattern().matcher(requestPath).matches())
                        .map(mapping -> mapping.getUrl() + requestPathWithQuery)
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("No mapping for request path: " + requestPath)));

                HttpClientRequest clientRequest = httpClient.request(serverRequest.method(), backendUrl.getPort(), backendUrl.getHost(), serverRequest.uri(), clientResponse -> {
                    HttpServerResponse serverResponse = serverRequest.response()
                            .setChunked(true)
                            .setStatusCode(clientResponse.statusCode());
                    serverResponse.headers().setAll(clientResponse.headers());
                    clientResponse.handler(data -> {
                        write(received, data);
                        serverResponse.write(data);
                    }).exceptionHandler(throwable -> {
                        actionService.endRequest(entry, backendUrl, throwable);
                        serverResponse.close();
                    }).endHandler(v -> {
                        actionService.endRequest(entry, clientResponse.statusCode(), clientResponse.getHeader("Content-Type"), backendUrl, sent.toString(), received.toString());
                        serverResponse.end();
                    });
                }).exceptionHandler(throwable -> {
                    actionService.endRequest(entry, backendUrl, throwable);
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
                actionService.endRequest(entry, e);

            }
        }).listen(proxyProperties.getPort(), result -> {
            if (result.succeeded()) {
                logger.info(String.format("HTTP proxy started on port %d", proxyProperties.getPort()));
            } else {
                logger.fatal("Unable to start HTTP proxy", result.cause());
            }
        });
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
