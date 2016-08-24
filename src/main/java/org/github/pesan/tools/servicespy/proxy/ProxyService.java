package org.github.pesan.tools.servicespy.proxy;

import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.github.pesan.tools.servicespy.action.ActionService;
import org.github.pesan.tools.servicespy.action.entry.RequestEntry;
import org.github.pesan.tools.servicespy.action.entry.ResponseDataEntry;
import org.github.pesan.tools.servicespy.action.entry.ResponseExceptionEntry;
import org.github.pesan.tools.servicespy.proxy.HttpServerBindings.Binding;
import org.springframework.beans.factory.annotation.Autowired;
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

import static java.util.stream.Collectors.*;

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
        proxyServers.stream().forEach(this::startServer);
    }

	private void startServer(Binding server) {
		server.getServer()
			.requestHandler(this::handleRequest)
			.listen(listenHandlerForServer(server));
	}

    private void handleRequest(HttpServerRequest serverRequest) {
        String requestPath = serverRequest.path();
        String requestPathWithQuery = serverRequest.uri();
        ByteArrayOutputStream received = new ByteArrayOutputStream();
        ByteArrayOutputStream sent = new ByteArrayOutputStream();

        RequestEntry reqEntry = new RequestEntry(requestPath, requestPathWithQuery, serverRequest.method().name(), parseHeaders(serverRequest.headers()), sent, getClockTime());
        try {
            URL backendUrl = createURL(proxyProperties.getMappings().stream()
                    .filter(ProxyProperties.Mapping::isActive)
                    .filter(mapping -> mapping.getPattern().matcher(requestPath).matches())
                    .map(mapping -> mapping.getUrl() + requestPathWithQuery)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No mapping for request path: " + requestPath)));

            HttpClient client = proxyClients.getByScheme(backendUrl.getProtocol());
            HttpClientRequest clientRequest = client.request(serverRequest.method(), getPort(backendUrl), backendUrl.getHost(), serverRequest.uri(), clientResponse -> {
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
                    actionService.log(reqEntry, new ResponseDataEntry(clientResponse.statusCode(), clientResponse.getHeader("Content-Type"), backendUrl, parseHeaders(clientResponse.headers()), received.toByteArray(), getClockTime()));
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

    private int getPort(URL backendUrl) {
        return backendUrl.getPort() != -1 ? backendUrl.getPort() : backendUrl.getDefaultPort();
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
			    logger.info(String.format(
			    		"Proxy server '%s' listening on %s:%d",
			    		name, host, port
			    ));
			} else {
			    logger.fatal(String.format(
			    		"Unable to start '%s' proxy server on %s:%d",
			    		name, host, port
			    ), result.cause());
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
