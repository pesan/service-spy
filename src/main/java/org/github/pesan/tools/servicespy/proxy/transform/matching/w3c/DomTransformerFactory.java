package org.github.pesan.tools.servicespy.proxy.transform.matching.w3c;

import org.github.pesan.tools.servicespy.proxy.transform.StreamTransformer;
import org.github.pesan.tools.servicespy.proxy.transform.matching.MatchableDocumentTransformer;
import org.github.pesan.tools.servicespy.proxy.transform.matching.MatchingTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;

@Component
public class DomTransformerFactory {
    private final DocumentBuilder builder;
    private final Transformer marshaller;

    @Autowired
    public DomTransformerFactory(DocumentBuilder builder, Transformer marshaller) {
        this.builder = builder;
        this.marshaller = marshaller;
    }

    public StreamTransformer createMatchingTransformer(MatchableDocumentTransformer<Document>... transformers) {
        return createMatchingTransformer(asList(transformers));
    }

    public StreamTransformer createMatchingTransformer(List<MatchableDocumentTransformer<Document>> transformers) {
        return new MatchingTransformer<>(
                transformers,
                (document, out) -> {
                    try {
                        marshaller.transform(new DOMSource(document), new StreamResult(out));
                    } catch (TransformerException e) {
                        throw new IOException(e);
                    }
                },
                in -> {
                    try {
                        return builder.parse(in);
                    } catch (SAXException e) {
                        throw new IOException(e);
                    }
                }
        );
    }
}
