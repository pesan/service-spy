package org.github.pesan.tools.servicespy.proxy.transform.matching.misc;

import org.github.pesan.tools.servicespy.proxy.transform.StreamTransformer;
import org.github.pesan.tools.servicespy.proxy.transform.matching.MatchableDocumentTransformer;
import org.github.pesan.tools.servicespy.proxy.transform.matching.MatchingTransformer;
import org.github.pesan.tools.servicespy.proxy.transform.matching.TransformerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.nio.charset.Charset;
import java.util.List;

@Component
public class StringTransformerFactory implements TransformerFactory<String> {
    private Charset charset = Charset.forName("UTF-8");

    @Override
    public StreamTransformer createMatchingTransformer(List<MatchableDocumentTransformer<String>> transformers) {
        return new MatchingTransformer<>(
                transformers,
                (d, out) -> StreamUtils.copy(d, charset, out),
                in -> StreamUtils.copyToString(in, charset)
        );
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }
}
