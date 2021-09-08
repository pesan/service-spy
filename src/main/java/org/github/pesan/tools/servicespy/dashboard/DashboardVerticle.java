package org.github.pesan.tools.servicespy.dashboard;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import org.github.pesan.tools.servicespy.dashboard.config.ConfigFeature;
import org.github.pesan.tools.servicespy.dashboard.traffic.TrafficFeature;
import org.github.pesan.tools.servicespy.util.Futures;

import java.util.Optional;
import java.util.function.Function;

public class DashboardVerticle extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        EventBus eventBus = vertx.eventBus();
        Settings settings = Json.toSettings(config().getJsonObject("server"));

        Futures.zip(
                        new TrafficFeature(vertx, config()).router(),
                        new ConfigFeature(vertx, config()).router(),
                        new StaticContentFeature(vertx, config()).router(),
                        this::assembleRouter)
                .compose(createServer(settings.getServerPort()))
                .onSuccess(httpServer -> {
                    eventBus.publish("dashboard.started", new JsonObject()
                            .put("port", httpServer.actualPort()));
                })
                .<Void>mapEmpty()
                .onComplete(startPromise);
    }

    private Router assembleRouter(Router trafficRouter, Router configRouter, Optional<Router> staticContentRouterOrEmpty) {
        Router rootRouter = Router.router(vertx);
        rootRouter.mountSubRouter("/api/traffic", trafficRouter);
        rootRouter.mountSubRouter("/api/config", configRouter);
        staticContentRouterOrEmpty.ifPresent(staticContentRouter ->
                rootRouter.mountSubRouter("/", staticContentRouter));
        return rootRouter;
    }

    private Function<Router, Future<HttpServer>> createServer(int serverPort) {
        return router -> vertx.createHttpServer()
                .requestHandler(router)
                .listen(serverPort);
    }
}