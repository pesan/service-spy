package org.github.pesan.tools.servicespy.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SpyingInputStream extends InputStream {
    private final InputStream stream;
    private final OutputStream spy;

    public SpyingInputStream(InputStream stream, OutputStream spy) {
        this.stream = stream;
        this.spy = spy;
    }

    @Override
    public int read() throws IOException {
        int c = stream.read();
        spy.write(c);
        return c;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int length = stream.read(b);
        if (length > 0) spy.write(b, 0, length);
        return length;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int length = stream.read(b, off, len);
        if (length > 0) spy.write(b, off, length);
        return length;
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }
}
