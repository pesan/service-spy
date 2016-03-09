package org.github.pesan.tools.servicespy.proxy;

import java.io.IOException;
import java.io.OutputStream;

public class SpyingOutputStream extends OutputStream {
    private final OutputStream stream;
    private final OutputStream spy;

    public SpyingOutputStream(OutputStream stream, OutputStream spy) {
        this.stream = stream;
        this.spy = spy;
    }

    @Override
    public void write(int b) throws IOException {
        spy.write(b);
        stream.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        spy.write(b);
        stream.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        spy.write(b, off, len);
        stream.write(b, off, len);
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }
}
