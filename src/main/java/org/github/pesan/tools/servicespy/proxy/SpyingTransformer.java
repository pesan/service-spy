package org.github.pesan.tools.servicespy.proxy;

import org.github.pesan.tools.servicespy.proxy.transform.StreamTransformer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public class SpyingTransformer implements StreamTransformer {
    private final StreamTransformer transformer;
    private final OutputStream before;
    private final OutputStream after;

    public SpyingTransformer(StreamTransformer transformer, OutputStream before, OutputStream after) {
        this.transformer = transformer;
        this.before = before;
        this.after = after;
    }

    @Override
    public void transform(InputStream in, OutputStream out, Map<String, Object> context) throws IOException {
        transformer.transform(new SpyingInputStream(in, before), new SpyingOutputStream(out, after), context);
    }
}
