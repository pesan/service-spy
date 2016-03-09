package org.github.pesan.tools.servicespy.proxy.transform;

import java.util.Map;

@FunctionalInterface
public interface DocumentTransformer<U> {
    U transform(U document, Map<String, Object> context);
}
