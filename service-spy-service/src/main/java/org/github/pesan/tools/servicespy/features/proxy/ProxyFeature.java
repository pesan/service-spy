package org.github.pesan.tools.servicespy.features.proxy;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableSource;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Function;
import org.apache.logging.log4j.util.Strings;
import org.github.pesan.platform.Feature;
import org.github.pesan.platform.Platform;
import org.github.pesan.platform.messaging.MessageBus;
import org.github.pesan.platform.web.HttpHeaders;
import org.github.pesan.platform.web.KeystoreConfig;
import org.github.pesan.platform.web.RequestHandler;
import org.github.pesan.platform.web.WebClient;
import org.github.pesan.platform.web.WebPlatform;
import org.github.pesan.platform.web.WebRequest;
import org.github.pesan.platform.web.WebResponse;
import org.github.pesan.platform.web.WebServer;
import org.github.pesan.tools.servicespy.application.ExceptionDetails;
import org.github.pesan.tools.servicespy.application.ProxyConfig;
import org.github.pesan.tools.servicespy.application.ProxyServerDto;
import org.github.pesan.tools.servicespy.application.RequestId;
import org.github.pesan.tools.servicespy.application.event.RequestDataEntry;
import org.github.pesan.tools.servicespy.application.event.RequestDataEntryDto;
import org.github.pesan.tools.servicespy.application.event.ResponseDataEntry;
import org.github.pesan.tools.servicespy.application.event.ResponseDataEntryDto;
import org.github.pesan.tools.servicespy.application.event.TEvent;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

@SuppressWarnings("unused")
public class ProxyFeature implements Feature {

    private final ProxyConfig config;

    @SuppressWarnings("unused")
    public ProxyFeature(ProxyConfig config) {
        this.config = config;
    }

    @Override
    public Completable initialize(Platform platform) {

        WebClient webClient = platform.web().webClient(config.idleTimeout(), config.connectionTimeout());
        WebClient secureWebClient = platform.web().secureWebClient(config.idleTimeout(), config.connectionTimeout());

        platform.messageBus().publish("proxy.init", config).subscribe();

        ProxyEvents events = new ProxyEvents(platform);

        Function<ProxyConfig, CompletableSource> serverLoader =
                settings -> reloadServers(webClient, secureWebClient, settings, platform, events);

        platform.messageBus().consumer(ProxyConfig.class, "proxy.settings")
                .flatMapCompletable(serverLoader)
                .subscribe();

        return Single.just(config)
                .flatMapCompletable(serverLoader);
    }

    private static class ProxyEvents {
        private final MessageBus.Publisher<TEvent.RequestBegin> onRequestBegin;
        private final MessageBus.Publisher<TEvent.RequestData> onRequestData;
        private final MessageBus.Publisher<TEvent.RequestEnd> onRequestEnd;
        private final MessageBus.Publisher<TEvent.RequestError> onRequestError;
        private final MessageBus.Publisher<TEvent.ResponseBegin> onResponseBegin;
        private final MessageBus.Publisher<TEvent.ResponseData> onResponseData;
        private final MessageBus.Publisher<TEvent.ResponseEnd> onResponseEnd;
        private final MessageBus.Publisher<TEvent.ResponseError> onResponseError;

        public ProxyEvents(Platform platform) {
            this.onRequestBegin = platform.messageBus().publisher("request.begin");
            this.onRequestData = platform.messageBus().publisher("request.data");
            this.onRequestEnd = platform.messageBus().publisher("request.end");
            this.onRequestError = platform.messageBus().publisher("request.error");
            this.onResponseBegin = platform.messageBus().publisher("response.begin");
            this.onResponseData = platform.messageBus().publisher("response.data");
            this.onResponseEnd = platform.messageBus().publisher("response.end");
            this.onResponseError = platform.messageBus().publisher("response.error");
        }

        public Completable onRequestBegin(RequestId requestId, WebRequest request) {
            URI requestUri = URI.create(request.uri());
            HttpHeaders headers = request.headers();
            RequestDataEntryDto requestData = RequestDataEntryDto.fromModel(new RequestDataEntry(
                    requestUri.getPath(),
                    Optional.ofNullable(requestUri.getQuery()),
                    request.method(),
                    headers,
                    LocalDateTime.now(),
                    "click download".getBytes(UTF_8),
                    headers.getFirstHeader("content-type").orElse(""),
                    Optional.empty()
            ));
            return onRequestBegin.send(new TEvent.RequestBegin(requestId, requestData));
        }

        public Completable onRequestData(RequestId requestId, byte[] data) {
            return onRequestData.send(new TEvent.RequestData(requestId, data));
        }

        public Completable onRequestEnd(RequestId requestId) {
            return onRequestEnd.send(new TEvent.RequestEnd(requestId));
        }

        public Completable onRequestError(RequestId requestId) {
            return onRequestError.send(new TEvent.RequestError(requestId, new ExceptionDetails(
                    "error", "error", List.of()
            )));
        }

        public Completable onResponseBegin(RequestId requestId, String host, int port, WebRequest request, WebResponse response) {
            try {
                URL url = new URL("%s://%s:%d%s".formatted("http", host, port, request.uri()));
                HttpHeaders headers = response.headers();
                ResponseDataEntryDto responseData = ResponseDataEntryDto.fromModel(new ResponseDataEntry(
                        response.status(),
                        url,
                        headers,
                        headers.getFirstHeader("content-type").orElse(""),
                        "click download".getBytes(UTF_8),
                        LocalDateTime.now(),
                        Optional.empty()
                ));
                return onResponseBegin.send(new TEvent.ResponseBegin(requestId, responseData));
            } catch (MalformedURLException e) {
                return Completable.error(e);
            }
        }

        public Completable onResponseData(RequestId requestId, byte[] data) {
            return onResponseData.send(new TEvent.ResponseData(requestId, data));
        }

        public Completable onResponseEnd(RequestId requestId) {
            return onResponseEnd.send(new TEvent.ResponseEnd(requestId));
        }

        public Completable onResponseError(RequestId requestId) {
            return onResponseError.send(new TEvent.ResponseError(requestId, new ExceptionDetails(
                    "error", "error", List.of()
            )));
        }
    }

    private static Completable reloadServers(WebClient httpClient, WebClient httpsClient, ProxyConfig proxyProperties, Platform platform, ProxyEvents events) {
        return Observable.fromIterable(proxyProperties.servers().entrySet())
                .flatMapSingle(entry -> {
                    ProxyServerDto proxyServer = entry.getValue();

                    List<ProxyServerDto.MappingDto> activeMappings = proxyServer.mappings().stream()
                            .filter(ProxyServerDto.MappingDto::active)
                            .toList();

                    RequestHandler requestHandler = request -> activeMappings.stream()
                            .filter(mapping -> Pattern.compile(mapping.pattern()).matcher(request.uri()).matches())
                            .findFirst()
                            .map(mappingDto -> doProxy(mappingDto, request, httpClient, httpsClient, events))
                            .orElseGet(() -> Single.just(WebResponse.forStatus(501)));

                    return createServer(platform.web(), proxyServer, requestHandler);
                })
                .ignoreElements();
    }

    private static Single<WebServer> createServer(WebPlatform web, ProxyServerDto proxyServer, RequestHandler requestHandler) {
        if (proxyServer.ssl()) {
            KeystoreConfig keystoreConfig = getKeystoreConfig(proxyServer);
            return web.secureWebServer(proxyServer.host(), proxyServer.port(), keystoreConfig, requestHandler);
        }
        return web.webServer(proxyServer.host(), proxyServer.port(), requestHandler);
    }

    private static KeystoreConfig getKeystoreConfig(ProxyServerDto proxyServer) {
        ProxyServerDto.KeystoreConfig keystoreConfig = proxyServer.keystoreConfig();

        if (Strings.isNotBlank(keystoreConfig.jksKeystore())) {
            return new KeystoreConfig.JksFile(keystoreConfig.jksKeystore(), keystoreConfig.jksPassword());
        } else if (Strings.isNotBlank(keystoreConfig.pfxKeystore())) {
            return new KeystoreConfig.PfxFile(keystoreConfig.pfxKeystore(), keystoreConfig.pfxPassword());
        } else if (Strings.isNotBlank(keystoreConfig.pemKeyPath())) {
            return new KeystoreConfig.PemFile(keystoreConfig.pemKeyPath(), keystoreConfig.pemCertPath());
        } else {
            try (InputStream defaultKeyStream = requireNonNull(ProxyFeature.class.getResourceAsStream("/tls/default-key.pem"));
                 InputStream defaultCertStream = requireNonNull(ProxyFeature.class.getResourceAsStream("/tls/default-cert.pem"))) {
                return new KeystoreConfig.PemData(
                        defaultKeyStream.readAllBytes(),
                        defaultCertStream.readAllBytes()
                );
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private static Single<WebResponse> doProxy(ProxyServerDto.MappingDto mappingDto,
                                               WebRequest request,
                                               WebClient httpClient,
                                               WebClient httpsClient,
                                               ProxyEvents events) {
        RequestId requestId = RequestId.random();

        int port = mappingDto.url().getPort() >= 0 ? mappingDto.url().getPort() : mappingDto.url().getDefaultPort();
        String host = mappingDto.url().getHost();
        WebClient client = mappingDto.url().getProtocol().equals("https") ? httpsClient : httpClient;

        return events.onRequestBegin(requestId, request)
                .andThen(client.send(host, port, request.compose(r -> r.doOnNext(b -> events.onRequestData(requestId, b)))))
                .flatMap(resp -> events.onRequestEnd(requestId)
                        .andThen(events.onResponseBegin(requestId, host, port, request, resp))
                        .toSingleDefault(resp.compose(r -> r.doOnNext(b -> events.onResponseData(requestId, b))))
                        .flatMap(e -> events.onResponseEnd(requestId)
                                .toSingleDefault(e)))
                .doOnError(e -> events.onResponseError(requestId))
                .onErrorReturn(e -> WebResponse.forStatus(501));
    }

}