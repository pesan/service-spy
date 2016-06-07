package org.github.pesan.tools.servicespy.config;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.github.pesan.tools.servicespy.proxy.ProxyProperties;
import org.github.pesan.tools.servicespy.proxy.ProxyProperties.Mapping;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;

@RunWith(MockitoJUnitRunner.class)
public class ConfigControllerTest {

    private final ProxyProperties proxyProperties = new ProxyProperties();

    private @Mock Mapping mapping1;
    private @Mock Mapping mapping2;

    private ConfigController configController;

    @Before
    public void setup() {
        configController = new ConfigController(proxyProperties);
    }

    @Test
    public void shouldContainNoMappingsWhenInitialized() {
        assertThat(getProxyConfig().getMappings(), equalTo(emptyList()));
    }

    @Test
    public void shouldStoreMappingsWhenUpdatingProxyConfiguration() {
        ProxyProperties proxyConfig = new ProxyProperties();
        proxyConfig.getMappings().add(mapping1);
        proxyConfig.getMappings().add(mapping2);

        updateProxyConfig(proxyConfig);

        assertThat(getProxyConfig().getMappings(), equalTo(asList(
            mapping1,
            mapping2
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

    private HttpStatus updateProxyConfig(ProxyProperties proxyConfig) {
        return configController.put(proxyConfig).toBlocking().single();
    }

    private ProxyProperties getProxyConfig() {
        return configController.get().toBlocking().single();
    }
}
