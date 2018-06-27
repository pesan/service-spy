package org.github.pesan.tools.servicespy.config;

import org.github.pesan.tools.servicespy.proxy.ProxyProperties;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;

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

    @Test
    public void shouldReturnHttpNoContentWhenUpdatingProxyConfiguration() {
        ProxyProperties proxyConfig = new ProxyProperties();

        assertThat(
            updateProxyConfig(proxyConfig),
            equalTo(HttpStatus.NO_CONTENT)
        );
    }

    @Test
    public void shouldReturnHttpBadRequestWhenMappingUrlIsNotHttpOrHttps() {
        ProxyProperties proxyConfig = createProxyConfig();
        proxyConfig.getServers().get("http").getMappings().add(ftpMapping);

        assertThat(
            updateProxyConfig(proxyConfig),
            equalTo(HttpStatus.BAD_REQUEST)
        );
    }

    private HttpStatus updateProxyConfig(ProxyProperties proxyConfig) {
        return configController.put(proxyConfig).toBlocking().single();
    }

    private ProxyProperties createProxyConfig() {
        ProxyProperties proxyProperties = new ProxyProperties();
        proxyProperties.getServers().put("http", new ProxyServer());
        return proxyProperties;
    }

    private ProxyProperties getCurrentProxyConfig() {
        return configController.get().toBlocking().single();
    }
}
