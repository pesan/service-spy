package org.github.pesan.tools.servicespy;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.config.yaml.YamlProcessor;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Launcher;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.github.pesan.tools.servicespy.action.DashboardVerticle;
import org.github.pesan.tools.servicespy.proxy.ProxyVerticle;

public class MainVerticle extends AbstractVerticle {

    public static void main(String[] args) {
        Launcher.main(args);
    }

    @Override
    public void start(Promise<Void> startup) throws Exception {

        // Backwards compatibility
        ConfigStoreOptions builtInConfig = new ConfigStoreOptions()
                .setType("json")
                .setConfig(new JsonObject(YamlProcessor.YAML_MAPPER
                        .readTree(MainVerticle.class.getResourceAsStream("/application.yml")).toString()));

        // Backwards compatibility
        ConfigStoreOptions externalConfig = new ConfigStoreOptions()
                .setType("file")
                .setFormat("yaml")
                .setOptional(true)
                .setConfig(new JsonObject().put("path", "application.yml"));

        ConfigRetrieverOptions configRetrieverOptions = new ConfigRetrieverOptions()
                .addStore(builtInConfig)
                .addStore(externalConfig);

        ConfigRetriever configRetriever = ConfigRetriever.create(vertx, configRetrieverOptions);
        configRetriever.getConfig(result -> {
            if (result.failed()) {
                startup.fail(result.cause());
            } else {
                JsonObject config = result.result();
                CompositeFuture
                        .all(
                                vertx.deployVerticle(new ProxyVerticle(), new DeploymentOptions().setConfig(config.getJsonObject("proxy"))),
                                vertx.deployVerticle(new DashboardVerticle(), new DeploymentOptions().setConfig(config))
                        )
                        .<Void>mapEmpty()
                        .onComplete(startup);
            }
        });
    }
}