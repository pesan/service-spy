package org.github.pesan.tools.servicespy;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.port;
import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Map;

import org.github.pesan.tools.servicespy.action.RequestIdGenerator;
import org.github.pesan.tools.servicespy.proxy.ProxyProperties;
import org.github.pesan.tools.util.SslUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringJUnit4ClassRunner.class)
@WebIntegrationTest({"server.port=0", "proxy.servers.http.port=65080", "proxy.servers.https.port=65443"})
@SpringApplicationConfiguration({Application.class, ProxyTest.TestConfiguration.class})
public class ProxyTest {

    private @Autowired ProxyProperties proxyProperties;

    private static Clock clock = mock(Clock.class);

    private final RestTemplate rest = SslUtils.trustAll(new RestTemplate());

    @Before
    public void init() {
        proxyProperties.getMappings().get(0).setUrl("http://localhost:" + port);
        proxyProperties.getMappings().get(0).setPattern("/.*");

        when(clock.instant())
                .thenReturn(Instant.EPOCH)
                .thenReturn(Instant.EPOCH.plusMillis(74));

		when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
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
    public void shouldHaveActionDataWhenThereIsOneAction() throws IOException {
        rest.postForObject("http://localhost:65080/test/entity?withQuery", singletonMap("id", "10993"), Object.class);

        given()
        .when()
            .get("/api/actions")
        .then()
            .body("[0].id", equalTo("87ed7de7-d115-488a-b68a-a903ad308753"))
            .body("[0].responseTimeMillis", equalTo(74))
            .body("[0].request.requestPath", equalTo("/test/entity"))
            .body("[0].request.requestPathWithQuery", equalTo("/test/entity?withQuery"))
            .body("[0].request.httpMethod", equalTo("POST"))
            .body("[0].request.data", equalTo(toBase64("{\"id\":\"10993\"}")))
            .body("[0].request.time", equalTo("1970-01-01T00:00:00"))
            .body("[0].response.status", equalTo(201))
            .body("[0].response.contentType", equalTo("application/json; charset=UTF-8"))
            .body("[0].response.host", equalTo("localhost:" + port))
            .body("[0].response.port", equalTo(port))
            .body("[0].response.hostName", equalTo("localhost"))
            .body("[0].response.data", equalTo(toBase64("{\"data\":\"entity\"}")))
            .body("[0].response.time", equalTo("1970-01-01T00:00:00.074"))
            .statusCode(200);
    }

    @Test
    public void shouldProvideRequestExceptionWhenRequestProcessingFails() throws IOException {
        proxyProperties.getMappings().get(0).setPattern("$.");

        try {
        	rest.getForObject("http://localhost:65080/nomatch", Object.class);
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
            .body("[0].request.requestPathWithQuery", equalTo("/nomatch"))
            .body("[0].request.httpMethod", equalTo("GET"))
            .body("[0].request.data", equalTo(""))
            .body("[0].request.time", equalTo("1970-01-01T00:00:00"))
            .body("[0].request.exception.message", notNullValue())
            .body("[0].response.time", equalTo("1970-01-01T00:00:00.074"))
            .statusCode(200);
    }

    @Test
    public void shouldProvideResponseExceptionWhenProxiedCallFails() throws IOException {
        proxyProperties.getMappings().get(0).setUrl("http://localhost:0");

        try {
            rest.getForObject("http://localhost:65080/invalid", Object.class);
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
            .body("[0].request.requestPathWithQuery", equalTo("/invalid"))
            .body("[0].request.httpMethod", equalTo("GET"))
            .body("[0].request.data", equalTo(""))
            .body("[0].request.time", equalTo("1970-01-01T00:00:00"))
            .body("[0].response.host", equalTo("localhost:0"))
            .body("[0].response.port", equalTo(0))
            .body("[0].response.hostName", equalTo("localhost"))
            .body("[0].response.time", equalTo("1970-01-01T00:00:00.074"))
            .body("[0].response.exception.message", notNullValue())
            .statusCode(200);
    }


    @Test
    public void shouldRespondWhenMakingRequestToHttpsEndpoint() throws IOException {
        proxyProperties.getMappings().get(0).setUrl("http://localhost:" + port);

        rest.getForObject("https://localhost:65443/test/entity", Object.class);

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

    @Value("${local.server.port}")
    public void setPort(int serverPort) {
        port = serverPort;
    }

    @Configuration
    static class TestConfiguration {
        @Primary @Bean
        public Clock clock() {
            return clock;
        }

        @Primary @Bean
        public RequestIdGenerator requestIdGenerator() {
            return () -> "87ed7de7-d115-488a-b68a-a903ad308753";
        }
    }
}

@RestController
class TestController {
    @RequestMapping(value="/test/entity", produces="application/json")
    public ResponseEntity<Map<String, String>> entity() {
        return new ResponseEntity<>(singletonMap("data", "entity"), HttpStatus.CREATED);
    }
}

