package org.github.pesan.tools.servicespy.proxy.transform.matching;

import java.io.IOException;
import java.io.InputStream;

@FunctionalInterface
public interface DocumentDeserializer<U> {
    U deserialize(InputStream in) throws IOException;
}
