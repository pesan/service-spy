package org.github.pesan.tools.servicespy.proxy.transform.matching.xom;

import org.github.pesan.tools.servicespy.proxy.transform.StreamTransformer;
import org.github.pesan.tools.servicespy.proxy.transform.matching.MatchableDocumentTransformer;
import org.github.pesan.tools.servicespy.proxy.transform.matching.MatchingTransformer;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.ParsingException;
import nu.xom.Serializer;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;

@Component
public class XomTransformerFactory {
    public StreamTransformer createMatchingTransformer(MatchableDocumentTransformer<Document>... transformers) {
        return createMatchingTransformer(asList(transformers));
    }

    public StreamTransformer createMatchingTransformer(List<MatchableDocumentTransformer<Document>> transformers) {
        return new MatchingTransformer<>(
                transformers,
                (document, out) -> {
                    new Serializer(out).write(document);
                },
                in -> {
                    try {
                        return new Builder().build(in);
                    } catch (ParsingException e) {
                        throw new IOException(e);
                    }
                }
        );
    }
}
