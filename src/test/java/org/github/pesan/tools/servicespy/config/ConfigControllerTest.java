package org.github.pesan.tools.servicespy.config;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    private @Mock Mapping httpMapping;
    private @Mock Mapping httpsMapping;

    private ConfigController configController;

    @Before
    public void setup() {
    	when(httpMapping.getUrl()).thenReturn("http://mapping1");
    	when(httpsMapping.getUrl()).thenReturn("https://mapping2");
        configController = new ConfigController(proxyProperties);
    }

    @Test
    public void shouldContainNoMappingsWhenInitialized() {
        assertThat(getProxyConfig().getMappings(), equalTo(emptyList()));
    }

    @Test
    public void shouldStoreMappingsWhenUpdatingProxyConfiguration() {
        ProxyProperties proxyConfig = new ProxyProperties();
        proxyConfig.getMappings().add(httpMapping);
        proxyConfig.getMappings().add(httpsMapping);

        updateProxyConfig(proxyConfig);

        assertThat(getProxyConfig().getMappings(), equalTo(asList(
            httpMapping,
            httpsMapping
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
        ProxyProperties proxyConfig = new ProxyProperties();
        proxyConfig.getMappings().add(mapping("ftp://host.com"));

        assertThat(
            updateProxyConfig(proxyConfig),
            equalTo(HttpStatus.BAD_REQUEST)
        );
    }

    private Mapping mapping(String url) {
    	Mapping mapping = mock(Mapping.class);
    	when(mapping.getUrl()).thenReturn(url);
		return mapping;
	}

	private HttpStatus updateProxyConfig(ProxyProperties proxyConfig) {
        return configController.put(proxyConfig).toBlocking().single();
    }

    private ProxyProperties getProxyConfig() {
        return configController.get().toBlocking().single();
    }
}
