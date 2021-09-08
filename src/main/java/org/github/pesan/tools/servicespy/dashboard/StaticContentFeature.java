package org.github.pesan.tools.servicespy.dashboard;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

import java.util.Optional;

public class StaticContentFeature {
    private final Vertx vertx;
    private final JsonObject config;

    public StaticContentFeature(Vertx vertx, JsonObject config) {
        this.vertx = vertx;
        this.config = config;
    }

    public Future<Optional<Router>> router() {
        Settings settings = Json.toSettings(config.getJsonObject("server"));
        return Future.succeededFuture(
                settings.getWebroot()
                        .map(webroot -> {
                            Router router = Router.router(vertx);
                            router.get("/*").handler(StaticHandler.create()
                                    .setAllowRootFileSystemAccess(true)
                                    .setWebRoot(webroot)
                                    .setDirectoryListing(false)
                            );
                            return router;
                        })
        );
    }
}