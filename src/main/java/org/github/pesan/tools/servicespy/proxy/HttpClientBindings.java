package org.github.pesan.tools.servicespy.proxy;

import io.vertx.core.http.HttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class HttpClientBindings {

    private final Map<String, HttpClient> clientsByScheme = new HashMap<>();

    @Autowired
    public HttpClientBindings(HttpClient httpClient,
            @Qualifier("https") HttpClient httpsClient) {
        clientsByScheme.put("http", httpClient);
        clientsByScheme.put("https", httpsClient);
    }

    public HttpClient getByScheme(String scheme) {
        HttpClient client = clientsByScheme.get(scheme);
        if (client == null) {
            throw new IllegalArgumentException(String.format(
                    "expected scheme from '%s', got '%s'",
                    clientsByScheme.keySet(), scheme
            ));
        }
        return client;
    }

}
