package org.github.pesan.platform.vertx;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PfxOptions;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.core.http.HttpServerRequest;
import io.vertx.rxjava3.core.http.HttpServerResponse;
import io.vertx.rxjava3.core.streams.Pump;
import io.vertx.rxjava3.ext.web.Router;
import io.vertx.rxjava3.impl.ReadStreamSubscriber;
import org.github.pesan.platform.Platform;
import org.github.pesan.platform.web.KeystoreConfig;
import org.github.pesan.platform.web.RequestHandler;
import org.github.pesan.platform.web.RequestPath;
import org.github.pesan.platform.web.Route;
import org.github.pesan.platform.web.Routes;
import org.github.pesan.platform.web.WebClient;
import org.github.pesan.platform.web.WebPlatform;
import org.github.pesan.platform.web.WebServer;

import java.util.concurrent.CompletableFuture;

class VertxWebPlatform implements WebPlatform {
    private final Vertx vertx;

    VertxWebPlatform(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public Single<WebServer> webServer(String host, int port, RequestHandler handler) {
        io.vertx.rxjava3.core.http.HttpServer httpServer = vertx.createHttpServer(new HttpServerOptions()
                .setHost(host)
                .setPort(port));

        httpServer
                .requestHandler(request -> handleRequest(request, request.response(), handler, RequestPath.empty())
                        .subscribe(
                                () -> request.response().end(),
                                Platform::unhandled
                        ));

        return httpServer.listen().map(server -> server::actualPort);
    }

    @Override
    public RequestHandler staticHandler(String webroot) {
        return new StaticFileRequestHandler(webroot);
    }

    @Override
    public Single<WebServer> secureWebServer(String host, int port, KeystoreConfig keystoreConfig, RequestHandler handler) {
        HttpServerOptions options = new HttpServerOptions()
                .setHost(host)
                .setPort(port)
                .setSsl(true);

        keystoreConfig.handle(
                jksFile -> options.setKeyStoreOptions(new JksOptions()
                        .setPath(jksFile.keystorePath())
                        .setPassword(jksFile.password())),
                pfxFile -> options.setPfxKeyCertOptions(new PfxOptions()
                        .setPath(pfxFile.keystorePath())
                        .setPassword(pfxFile.password())),
                pemFile -> options.setPemKeyCertOptions(new PemKeyCertOptions()
                        .setCertPath(pemFile.certPath())
                        .setKeyPath(pemFile.keyPath())),
                pemData -> options.setPemKeyCertOptions(new PemKeyCertOptions()
                        .setCertValue(io.vertx.core.buffer.Buffer.buffer(pemData.certData()))
                        .setKeyValue(io.vertx.core.buffer.Buffer.buffer(pemData.keyData()))));

        io.vertx.rxjava3.core.http.HttpServer httpServer = vertx.createHttpServer(options);

        httpServer
                .requestHandler(request -> handleRequest(request, request.response(), handler, RequestPath.empty())
                        .subscribe(
                                () -> request.response().end(),
                                Platform::unhandled
                        ));

        return httpServer.listen().map(server -> server::actualPort);
    }

    @Override
    public Single<WebServer> webServer(String host, int port, Routes routes) {
        Router router = Router.router(vertx);
        routes.stream()
                .forEach(route -> setupRoute(router, route));
        return vertx.createHttpServer()
                .requestHandler(router)
                .listen(port)
                .map(server -> server::actualPort);
    }

    private void setupRoute(Router router, Route route) {
        String path = route.getPath().asRaw();
        io.vertx.rxjava3.ext.web.Route vertxRoute = router.route(HttpMethod.valueOf(route.getMethod()), path);
        route.getProduces().ifPresent(vertxRoute::produces);
        route.getConsumes().ifPresent(vertxRoute::consumes);

        if (route.getHandler() instanceof StaticFileRequestHandler staticHandler) {
            vertxRoute.handler(io.vertx.rxjava3.ext.web.handler.StaticHandler.create(staticHandler.getWebroot()));
        } else {
            vertxRoute.handler(rc ->
                    handleRequest(rc.request(), rc.response(), route.getHandler(), route.getPath())
                            .andThen(Completable.defer(rc::end))
                            .subscribe(() -> {
                            }, Platform::unhandled)
            );
        }
    }

    private Completable handleRequest(HttpServerRequest request, HttpServerResponse serverResponse, RequestHandler requestHandler, RequestPath matchedPath) {
        return requestHandler
                .handle(Adapter.fromServerRequest(request, matchedPath))
                .flatMapCompletable(response -> {
                    response.headers().stream()
                            .forEach(header -> serverResponse.putHeader(header.getKey(), header.getValue()));
                    serverResponse
                            .setChunked(response.isChunked())
                            .setStatusCode(response.status());

                    CompletableFuture<Void> objectCompletableFuture = new CompletableFuture<>();

                    Flowable<Buffer> bufferFlowable = response.body().map(Buffer::buffer)
                            .doOnError(objectCompletableFuture::completeExceptionally)
                            .doOnComplete(() -> objectCompletableFuture.complete(null));
                    // Workaround: https://github.com/vert-x3/vertx-rx/issues/123
                    Pump.newInstance(
                            io.vertx.core.streams.Pump.pump(
                                    ReadStreamSubscriber.asReadStream(bufferFlowable, Buffer::getDelegate),
                                    serverResponse.getDelegate()
                            )).start();

                    return Completable.fromCompletionStage(objectCompletableFuture);
                });
    }

    @Override
    public WebClient webClient(int idleTimeout, int connectionTimeout) {
        HttpClientOptions httpClientOptions = new HttpClientOptions()
                .setIdleTimeout(idleTimeout / 1000)
                .setConnectTimeout(connectionTimeout);
        return new VertxWebClient(vertx.createHttpClient(httpClientOptions));
    }

    @Override
    public WebClient secureWebClient(int idleTimeout, int connectionTimeout) {
        HttpClientOptions httpClientOptions = new HttpClientOptions()
                .setIdleTimeout(idleTimeout / 1000)
                .setConnectTimeout(connectionTimeout)
                .setSsl(true)
                .setTrustAll(true)
                .setVerifyHost(false);
        return new VertxWebClient(vertx.createHttpClient(httpClientOptions));
    }

}
