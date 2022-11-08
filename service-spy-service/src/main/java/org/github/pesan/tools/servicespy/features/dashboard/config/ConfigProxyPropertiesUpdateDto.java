package org.github.pesan.tools.servicespy.features.dashboard.config;

import org.github.pesan.tools.servicespy.application.ProxyServerDto;

import java.util.List;
import java.util.Map;

public record ConfigProxyPropertiesUpdateDto(Map<String, List<ProxyServerDto.MappingDto>> mappings) {
}