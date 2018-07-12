package org.github.pesan.tools.servicespy;

import com.jayway.restassured.http.ContentType;
import org.github.pesan.tools.servicespy.action.RequestIdGenerator;
import org.github.pesan.tools.servicespy.proxy.ProxyProperties;
import org.github.pesan.tools.util.SslUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Map;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.port;
import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.mockito.Mockito.when;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "proxy.servers.http.port=31080",
                "proxy.servers.https.port=31443"
        }
)
public class ProxyTest {

    private @Autowired ProxyProperties proxyProperties;

    @MockBean
    private Clock clock;

    @MockBean
    private RequestIdGenerator requestIdGenerator;

    private final RestTemplate rest = SslUtils.trustAll(new RestTemplate());

    @Before
    public void init() {
        proxyProperties.getServers().get("http").getMappings().get(0).setUrl("http://localhost:" + port);
        proxyProperties.getServers().get("http").getMappings().get(0).setPattern("/.*");
        proxyProperties.getServers().get("https").getMappings().get(0).setUrl("http://localhost:" + port);
        proxyProperties.getServers().get("https").getMappings().get(0).setPattern("/.*");

        when(clock.instant())
                .thenReturn(Instant.EPOCH)
                .thenReturn(Instant.EPOCH.plusMillis(74));

        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
        when(requestIdGenerator.next()).thenReturn("87ed7de7-d115-488a-b68a-a903ad308753");
        given().delete("/api/actions");
    }

    @Test
    public void shouldHaveEmptyResponseWhenThereIsNoActions() {
        given()
        .when()
            .get("/api/actions")
        .then()
            .body(equalTo("[]"))
            .statusCode(200);
    }

    @Test
    public void shouldReturnDataAndContentTypeWhenFetchingResponseData() {

        rest.getForObject("http://localhost:31080/test/entity", Object.class);

        given()
        .when()
           .get("/api/actions/87ed7de7-d115-488a-b68a-a903ad308753/data/response/")
        .then()
            .body(equalTo("{\"data\":\"entity\"}"))
            .contentType(ContentType.JSON)
            .statusCode(200);
    }

    @Test
    public void shouldHaveActionDataWhenThereIsOneAction() {
        rest.postForObject("http://localhost:31080/test/entity?withQuery&page=1", singletonMap("id", "10993"), Map.class);

        given()
        .when()
            .get("/api/actions")
        .then()
            .body("[0].id", equalTo("87ed7de7-d115-488a-b68a-a903ad308753"))
            .body("[0].responseTimeMillis", equalTo(74))
            .body("[0].request.requestPath", equalTo("/test/entity"))
            .body("[0].request.query", equalTo("?withQuery&page=1"))
            .body("[0].request.httpMethod", equalTo("POST"))
            .body("[0].request.contentType", equalTo("application/json;charset=UTF-8"))
            .body("[0].request.data", equalTo(toBase64("{\"id\":\"10993\"}")))
            .body("[0].request.time", equalTo("1970-01-01T00:00:00"))
            .body("[0].response.status", equalTo(201))
            .body("[0].response.contentType", equalTo("application/json;charset=utf-8"))
            .body("[0].response.host", equalTo("localhost:" + port))
            .body("[0].response.port", equalTo(port))
            .body("[0].response.hostName", equalTo("localhost"))
            .body("[0].response.data", equalTo(toBase64("{\"data\":{\"id\":\"10993\"}}")))
            .body("[0].response.time", equalTo("1970-01-01T00:00:00.074"))
            .body("[0].href.requestData", equalTo("/api/actions/87ed7de7-d115-488a-b68a-a903ad308753/data/request/"))
            .body("[0].href.responseData", equalTo("/api/actions/87ed7de7-d115-488a-b68a-a903ad308753/data/response/"))
            .statusCode(200);
    }

    @Test
    public void shouldProvideRequestExceptionWhenRequestProcessingFails() {
        proxyProperties.getServers().get("http").getMappings().get(0).setPattern("$.");

        try {
            rest.getForObject("http://localhost:31080/nomatch", Object.class);
        } catch (ResourceAccessException ignored) {
            // expected
        }

        given()
        .when()
            .get("/api/actions")
        .then()
            .body("[0].id", equalTo("87ed7de7-d115-488a-b68a-a903ad308753"))
            .body("[0].responseTimeMillis", equalTo(74))
            .body("[0].request.requestPath", equalTo("/nomatch"))
            .body("[0].request.query", equalTo(""))
            .body("[0].request.httpMethod", equalTo("GET"))
            .body("[0].request.data", equalTo(""))
            .body("[0].request.time", equalTo("1970-01-01T00:00:00"))
            .body("[0].request.exception.name", notNullValue())
            .body("[0].request.exception.message", notNullValue())
            .body("[0].response.time", equalTo("1970-01-01T00:00:00.074"))
            .statusCode(200);
    }

    @Test
    public void shouldProvideResponseExceptionWhenProxiedCallFails() {
        proxyProperties.getServers().get("http").getMappings().get(0).setUrl("http://localhost:0");

        try {
            rest.getForObject("http://localhost:31080/invalid", Object.class);
        } catch (ResourceAccessException ignored) {
            // expected
        }

        given()
        .when()
            .get("/api/actions")
        .then()
            .body("[0].id", equalTo("87ed7de7-d115-488a-b68a-a903ad308753"))
            .body("[0].responseTimeMillis", equalTo(74))
            .body("[0].request.requestPath", equalTo("/invalid"))
            .body("[0].request.query", equalTo(""))
            .body("[0].request.httpMethod", equalTo("GET"))
            .body("[0].request.data", equalTo(""))
            .body("[0].request.time", equalTo("1970-01-01T00:00:00"))
            .body("[0].response.host", equalTo("localhost:0"))
            .body("[0].response.port", equalTo(0))
            .body("[0].response.hostName", equalTo("localhost"))
            .body("[0].response.time", equalTo("1970-01-01T00:00:00.074"))
            .body("[0].response.exception.name", notNullValue())
            .body("[0].response.exception.message", notNullValue())
            .statusCode(200);
    }

    @Test
    public void shouldRespondWhenMakingRequestToHttpsEndpoint() {

        rest.getForObject("https://localhost:31443/test/entity", Object.class);

        given()
        .when()
            .get("/api/actions")
        .then()
            .body("[0].id", equalTo("87ed7de7-d115-488a-b68a-a903ad308753"))
            .statusCode(200);
    }

    private String toBase64(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(Charset.forName("UTF-8")));
    }

    @LocalServerPort
    public void setPort(int serverPort) {
        port = serverPort;
    }
}

@RestController
class TestController {
    @RequestMapping(value="/test/entity", produces="application/json", method=GET)
    public ResponseEntity<Map<String, String>> getEntity() {
        return new ResponseEntity<>(singletonMap("data", "entity"), HttpStatus.OK);
    }

    @RequestMapping(value="/test/entity", produces="application/json", consumes="application/json", method=POST)
    public ResponseEntity<Map<String, Map<String, String>>> addEntity(@RequestBody Map<String, String> entity) {
        return new ResponseEntity<>(singletonMap("data", entity), HttpStatus.CREATED);
    }
}

