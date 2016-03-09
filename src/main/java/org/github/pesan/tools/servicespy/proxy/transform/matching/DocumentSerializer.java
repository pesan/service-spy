package org.github.pesan.tools.servicespy.proxy.transform.matching;

import java.io.IOException;
import java.io.OutputStream;

@FunctionalInterface
public interface DocumentSerializer<U> {
    void serialize(U document, OutputStream out) throws IOException;
}
