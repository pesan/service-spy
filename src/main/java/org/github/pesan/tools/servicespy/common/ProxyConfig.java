package org.github.pesan.tools.servicespy.common;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
@ConfigurationProperties(prefix="proxy.config")
public class ProxyConfig {
    public static class Mapping {
        private Pattern pattern;
        private String url;
        private boolean active = true;

        public Pattern getPattern() { return pattern; }
        public void setPattern(String pattern) { this.pattern = Pattern.compile(pattern); }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }

    public static class RegexpTransform {
        private String servicePattern;
        private String matchPattern;
        private String replacement;
        private boolean active;

        public String getServicePattern() { return servicePattern; }
        public void setServicePattern(String servicePattern) { this.servicePattern = servicePattern; }
        public String getMatchPattern() { return matchPattern; }
        public void setMatchPattern(String matchPattern) { this.matchPattern = matchPattern; }
        public String getReplacement() { return replacement; }
        public void setReplacement(String replacement) { this.replacement = replacement; }

        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }

    private List<Mapping> mappings = new ArrayList<>();
    public List<Mapping> getMappings() { return mappings; }

    private List<RegexpTransform> responseTransforms = new ArrayList<>();
    public List<RegexpTransform> getResponseTransforms() { return responseTransforms; }

    private List<RegexpTransform> requestTransforms = new ArrayList<>();
    public List<RegexpTransform> getRequestTransforms() { return requestTransforms; }
}
