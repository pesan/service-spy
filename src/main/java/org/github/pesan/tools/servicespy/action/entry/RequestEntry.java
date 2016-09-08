package org.github.pesan.tools.servicespy.action.entry;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface RequestEntry {
    String getRequestPath();
    String getRequestPathWithQuery();
    String getHttpMethod();
    Map<String, List<String>> getHeaders();
    LocalDateTime getTime();
}
