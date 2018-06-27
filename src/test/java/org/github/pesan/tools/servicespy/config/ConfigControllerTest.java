package org.github.pesan.tools.servicespy.config;

import org.github.pesan.tools.servicespy.proxy.ProxyProperties;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConfigControllerTest {

    private @Mock ProxyServer.Mapping httpMapping;
    private @Mock ProxyServer.Mapping ftpMapping;
    private ConfigController configController;

    @Before
    public void setup() {
        when(httpMapping.getUrl()).thenReturn("http://mapping.url");
        when(ftpMapping.getUrl()).thenReturn("ftp://mapping.url");
        configController = new ConfigController(createProxyConfig());
    }

    @Test
    public void shouldContainNoMappingsWhenInitialized() {
        assertThat(getCurrentProxyConfig().getServers().get("http").getMappings(), equalTo(emptyList()));
    }

    @Test
    public void shouldStoreMappingsWhenUpdatingProxyConfiguration() {
        ProxyProperties proxyConfig = createProxyConfig();
        proxyConfig.getServers().get("http").getMappings().add(httpMapping);

        updateProxyConfig(proxyConfig);

        assertThat(getCurrentProxyConfig().getServers().get("http").getMappings(), equalTo(singletonList(
                httpMapping
        )));
    }

    @Test(expected = ResponseStatusException.class)
    public void shouldThrowBadRequestWhenReferencingUnknownProxyServer() {
        ProxyProperties proxyConfig = createProxyConfig();
        proxyConfig.getServers().computeIfAbsent("non-existing", __ -> new ProxyServer())
                   .getMappings().add(httpMapping);

        try {
            updateProxyConfig(proxyConfig);
        } catch (ResponseStatusException e) {
            assertThat(e.getStatus(), equalTo(HttpStatus.BAD_REQUEST));
            assertThat(e.getReason(), equalTo("unknown proxy server: non-existing"));
            throw e;
        }
    }

    @Test(expected = ResponseStatusException.class)
    public void shouldThrowHttpBadRequestWhenMappingUrlIsNotHttpOrHttps() {
        ProxyProperties proxyConfig = createProxyConfig();
        proxyConfig.getServers().get("http").getMappings().add(ftpMapping);

        try {
            updateProxyConfig(proxyConfig);
        } catch (ResponseStatusException e) {
            assertThat(e.getStatus(), equalTo(HttpStatus.BAD_REQUEST));
            assertThat(e.getReason(), equalTo("expected protocol from: http, https"));
            throw e;
        }
    }

    private void updateProxyConfig(ProxyProperties proxyConfig) {
        configController.put(proxyConfig).blockingAwait();
    }

    private ProxyProperties createProxyConfig() {
        ProxyProperties proxyProperties = new ProxyProperties();
        proxyProperties.getServers().put("http", new ProxyServer());
        return proxyProperties;
    }

    private ProxyProperties getCurrentProxyConfig() {
        return configController.get().blockingGet();
    }
}
