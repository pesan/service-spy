package org.github.pesan.tools.servicespy;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class TestMainVerticle {

    private int httpProxyPort;
    private int httpsProxyPort;
    private int dashboardPort;

    @BeforeEach
    void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
        vertx.eventBus().<JsonObject>consumer("dashboard.started", msg -> {
            dashboardPort = msg.body().getInteger("port");
        });
        vertx.eventBus().<JsonObject>consumer("proxy.started", msg -> {
            httpProxyPort = msg.body().getJsonObject("ports").getInteger("http");
            httpsProxyPort = msg.body().getJsonObject("ports").getInteger("https");
        });
        vertx.deployVerticle(
                new MainVerticle(),
                new DeploymentOptions()
                        .setConfig(new JsonObject()
                                .put("server", new JsonObject().put("port", 0))
                                .put("proxy", new JsonObject().put("servers", new JsonObject()
                                                .put("http", new JsonObject().put("port", 0))
                                                .put("https", new JsonObject().put("port", 0))
                                        ))
                        ))
                .onComplete(testContext.succeeding(id -> testContext.completeNow()));
    }


    @Test
    @DisplayName("Should have launched the dashboard when deploying MainVerticle")
    void dashboardDeployed(Vertx vertx, VertxTestContext testContext) {
        HttpClient client = vertx.createHttpClient();

        client.request(HttpMethod.GET, dashboardPort, "localhost", "/")
                .compose(HttpClientRequest::send)
                .onSuccess(response -> {
                    response.bodyHandler(__ -> testContext.completeNow());
                    response.end();
                })
                .onFailure(testContext::failNow);
    }

    @Test
    @DisplayName("Should have launched the http proxy when deploying MainVerticle")
    void httpProxyDeployed(Vertx vertx, VertxTestContext testContext) {
        HttpClient client = vertx.createHttpClient();

        client.request(HttpMethod.GET, httpProxyPort, "localhost", "/")
                .compose(HttpClientRequest::send)
                .onSuccess(response -> {
                    response.bodyHandler(__ -> testContext.completeNow());
                    response.end();
                })
                .onFailure(testContext::failNow);
    }

    @Test
    @DisplayName("Should have launched the https proxy when deploying MainVerticle")
    void httpsProxyDeployed(Vertx vertx, VertxTestContext testContext) {
        HttpClient client = vertx.createHttpClient();

        client.request(HttpMethod.GET, httpsProxyPort, "localhost", "/")
                .compose(HttpClientRequest::send)
                .onSuccess(response -> {
                    response.bodyHandler(__ -> testContext.completeNow());
                    response.end();
                })
                .onFailure(testContext::failNow);
    }
}