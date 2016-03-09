package org.github.pesan.tools.servicespy.proxy.transform;

import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

@FunctionalInterface
public interface StreamTransformer {
    StreamTransformer PASS_THROUGH = (in, out, context) -> StreamUtils.copy(in, out);

    void transform(InputStream in, OutputStream out, Map<String, Object> context) throws IOException;
}
