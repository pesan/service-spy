package org.github.pesan.tools.servicespy;

import io.reactivex.rxjava3.core.Completable;
import org.github.pesan.tools.servicespy.config.YamlConfig;
import org.junit.jupiter.api.Test;

//@ExtendWith(VertxExtension.class)
public class IntegrationTest {

    @Test
    void donothing() throws Exception {

        Completable run = ServiceSpy.run(YamlConfig.toObject(ServiceSpy.Config.class, YamlConfig.fromYaml("""
                platform: vertx
                features:
                  dashboard:
                    feature: dashboard
                    config:
                      port: 8080
                """).orElseThrow(e -> e)).orElseThrow(e -> e));

        run.test()
                .await()
                .hasSubscription();
    }

 /*   private Vertx vertx;
    private HttpClient httpClient;
    private HttpClient httpsClient;

    private Port httpPort;
    private Port httpsPort;
    private Port dashboardPort;
    private Port backingPort;

    @BeforeEach
    void setup(VertxTestContext testContext) {
        this.vertx = Vertx.vertx();

        vertx.eventBus().addOutboundInterceptor((DeliveryContext<JsonObject> msg) -> {
            JsonObject body = msg.message().body();
            if (msg.message().address().equals("proxy.started")) {
                JsonObject portsMap = body.getJsonObject("ports");
                httpPort = Port.of(portsMap.getInteger("http"));
                httpsPort = Port.of(portsMap.getInteger("https"));

                httpClient = vertx.createHttpClient(new HttpClientOptions()
                        .setDefaultPort(httpPort.portNumber()));
                httpsClient = vertx.createHttpClient(new HttpClientOptions()
                        .setDefaultPort(httpsPort.portNumber())
                        .setTrustAll(true)
                        .setSsl(true));
            }
            if (msg.message().address().equals("dashboard.started")) {
                dashboardPort = Port.of(body.getInteger("port"));
            }
            msg.next();
        });

        deployMockTarget()
                .doOnSuccess(port -> backingPort = port)
                .flatMapCompletable(this::deployMain)
                .subscribe(testContext::completeNow, testContext::failNow);
    }

    @Test
    void testGet(VertxTestContext testContext) {
        givenProxyRequestWasSent("GET", "/any/url", Map.of(
                "X-Test-Header", "Test-Header-Value",
                "Content-Type", "application/json"

        ), null);

        whenListeningToTrafficStream()
                .awaitCount(1)
                .assertValueCount(1)
                .assertValue(payload -> {
                    String requestId = payload.getString("id");
                    assertEquals("""
                            {
                              id: %1$s,
                              request: {
                                requestPath: "/any/url",
                                query: null,
                                httpMethod: "GET",
                                headers: {
                                  x-test-header: ["Test-Header-Value"],
                                  host: ["localhost:%3$s"]
                                },
                                data: "Y2xpY2sgZG93bmxvYWQ",
                                contentType: "application/json"
                              },
                              response: {
                                statusCode: 200,
                                url: "http://localhost:%2$s/any/url",
                                headers: {
                                  content-length: ["18"],
                                  x-responder-class: ["IntegrationTest"],
                                  content-type: ["text/plain"]
                                },
                                contentType: "text/plain",
                                data: "Y2xpY2sgZG93bmxvYWQ",
                                host: "localhost:%2$s",
                                hostName: "localhost",
                                port: %2$s
                              },
                              href: {
                                requestData: "/api/traffic/%1$s/data/request",
                                responseData: "/api/traffic/%1$s/data/response"
                              }
                            }""".formatted(requestId, backingPort, httpsPort), payload.encode(), LENIENT);
                    return true;
                });

        testContext.completeNow();
    }

    @Test
    void testPost(VertxTestContext testContext) {
        givenProxyRequestWasSent("POST", "/form/submit", Map.of(
                "X-Test-Header", "Test-Header-Value",
                "Content-Type", "application/json"
        ), "{\"index\": 1}");

        whenListeningToTrafficStream()
                .awaitCount(1).assertValueCount(1).assertValue(payload -> {
                    String requestId = payload.getString("id");
                    assertEquals("""
                            {
                              id: %1$s,
                              request: {
                                requestPath: "/form/submit",
                                query: null,
                                httpMethod: "POST",
                                headers: {
                                  x-test-header: ["Test-Header-Value"],
                                  host: ["localhost:%3$s"]
                                },
                                data: "Y2xpY2sgZG93bmxvYWQ",
                                contentType: "application/json"
                              },
                              response: {
                                statusCode: 200,
                                url: "http://localhost:%2$s/form/submit",
                                headers: {
                                  content-length: ["18"],
                                  x-responder-class: ["IntegrationTest"],
                                  content-type: ["text/plain"]
                                },
                                contentType: "text/plain",
                                data: "Y2xpY2sgZG93bmxvYWQ",
                                host: "localhost:%2$s",
                                hostName: "localhost",
                                port: %2$s
                              },
                              href: {
                                requestData: "/api/traffic/%1$s/data/request",
                                responseData: "/api/traffic/%1$s/data/response"
                              }
                            }""".formatted(requestId, backingPort, httpsPort), payload.encode(), LENIENT);
                    return true;
                });

        testContext.completeNow();
    }

    @Test
    void testGetRequestData(VertxTestContext testContext) {
        String payload = "{\"index\": 1}";
        givenProxyRequestWasSent("POST", "/form/submit", Map.of(
                "X-Test-Header", "Test-Header-Value",
                "Content-Type", "application/json"

        ), payload);

        whenRequestData()
                .awaitCount(1)
                .assertValueCount(1)
                .assertValue(v -> {
                    assertThat(new String(v, UTF_8)).isEqualTo(payload);
                    return true;
                });

        testContext.completeNow();
    }

    @Test
    void testGetResponseData(VertxTestContext testContext) {
        givenProxyRequestWasSent("GET", "/result", Map.of(
                "X-Test-Header", "Test-Header-Value"

        ), "");

        whenResponseData()
                .awaitCount(1)
                .assertValueCount(1)
                .assertValue(v -> {
                    assertThat(new String(v, UTF_8)).isEqualTo("{\"response\": true}");
                    return true;
                });

        testContext.completeNow();
    }

    private void givenProxyRequestWasSent(String method, String url, Map<String, String> headers, String payload) {
        httpsClient.request(HttpMethod.valueOf(method), "localhost", url)
                .map(request -> {
                    request.headers().addAll(headers);
                    return request;
                })
                .flatMap(request ->
                        payload != null ? request.send(payload) : request.send()
                )
                .test()
                .awaitDone(5, TimeUnit.SECONDS);
    }


    private TestSubscriber<JsonObject> whenListeningToTrafficStream() {
        return trafficStreamSubscription()
                .test();
    }

    private Flowable<JsonObject> trafficStreamSubscription() {
        return httpClient.request(HttpMethod.GET, dashboardPort.portNumber(), "localhost", "/api/traffic")
                .map(request -> request.putHeader("Accept", "text/event-stream"))
                .flatMap(HttpClientRequest::send)
                .doOnSuccess(this::assertOK)
                .toFlowable()
                .flatMap(HttpClientResponse::toFlowable)
                .map(this::getServerSentEventBody)
                .map(JsonObject::new);
    }

    private TestSubscriber<byte[]> whenRequestData() {
        return trafficStreamSubscription()
                .map(json -> json.getJsonObject("href").getString("requestData"))
                .flatMap(requestDataUri -> httpClient.request(HttpMethod.GET, dashboardPort.portNumber(), "localhost", requestDataUri)
                        .flatMap(HttpClientRequest::send)
                        .doOnSuccess(this::assertOK)
                        .toFlowable()
                        .flatMap(HttpClientResponse::toFlowable)
                        .map(Buffer::getBytes))
                .test();
    }

    private TestSubscriber<byte[]> whenResponseData() {
        return trafficStreamSubscription()
                .map(json -> json.getJsonObject("href").getString("responseData"))
                .flatMap(responseDataUri -> httpClient.request(HttpMethod.GET, dashboardPort.portNumber(), "localhost", responseDataUri)
                        .flatMap(HttpClientRequest::send)
                        .doOnSuccess(this::assertOK)
                        .toFlowable()
                        .flatMap(HttpClientResponse::toFlowable)
                        .map(Buffer::getBytes))
                .test();
    }

    private void assertOK(HttpClientResponse response) {
        assertThat(response.statusCode()).isEqualTo(200);
    }

    private String getServerSentEventBody(Buffer buffer) {
        String str = buffer.toString(UTF_8);
        assertThat(str).startsWith("data: ");
        return str.substring(6);
    }

    private Single<Port> deployMockTarget() {
        return vertx.createHttpServer()
                .requestHandler(request ->
                        request.body(payload -> request.response()
                                .putHeader("X-Responder-Class", getClass().getSimpleName())
                                .putHeader("Content-Type", "text/plain")
                                .setStatusCode(200)
                                .exceptionHandler(e -> fail("response", e))
                                .send("{\"response\": true}").subscribe()).exceptionHandler(e -> fail("request", e)))
                .listen(0)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .map(HttpServer::actualPort)
                .map(Port::of);
    }

    private Completable deployMain(Port port) {
        try {
            JsonObject testConfig = new JsonObject(
                    YamlProcessor.YAML_MAPPER.readTree(IntegrationTest.class.getResourceAsStream("/test.yml").readAllBytes())
                            .toString().replace("$proxyPort", port.asText()));
            return vertx.deployVerticle(new ServerVerticle(), new DeploymentOptions().setConfig(testConfig))
                    .ignoreElement();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
  */
}
