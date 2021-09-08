package org.github.pesan.tools.servicespy;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.github.pesan.tools.servicespy.proxy.ProxyVerticle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
public class TestProxyVerticle {

    @Test
    public void test(Vertx vertx, VertxTestContext context) {
        context.assertComplete(
                        vertx.createHttpServer()
                                .requestHandler(a -> {
                                    a.response().end("backend response");
                                })
                                .listen(0)
                )
                .onSuccess(backendServer -> {
                    JsonObject conf = new JsonObject()
                            .put("servers", new JsonObject().put("http", new JsonObject()
                                    .put("host", "localhost")
                                    .put("port", 0)
                                    .put("mappings", List.of(
                                            new JsonObject()
                                                    .put("url", "http://localhost:" + backendServer.actualPort())
                                                    .put("pattern", "/*")
                                    ))
                            ));

                    vertx.eventBus().<JsonObject>consumer("proxy.started", msg -> {
                        int proxyServerPort = msg.body().getJsonObject("ports").getInteger("http");

                        vertx.createHttpClient()
                                .request(HttpMethod.GET, proxyServerPort, "localhost", "/")
                                .compose(HttpClientRequest::send)
                                .onFailure(context::failNow)
                                .onSuccess(response -> {
                                    response.bodyHandler(buffer -> {
                                        context.verify(() -> {
                                            assertEquals("backend response", buffer.toString());
                                        });
                                        context.completeNow();
                                    });
                                });

                    });

                    vertx.deployVerticle(new ProxyVerticle(), new DeploymentOptions().setConfig(conf))
                            .onFailure(context::failNow);
                });
    }
}