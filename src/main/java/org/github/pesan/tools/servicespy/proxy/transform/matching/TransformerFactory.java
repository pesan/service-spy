package org.github.pesan.tools.servicespy.proxy.transform.matching;

import org.github.pesan.tools.servicespy.proxy.transform.StreamTransformer;

import java.util.List;

public interface TransformerFactory<U> {
    StreamTransformer createMatchingTransformer(List<MatchableDocumentTransformer<U>> transformers);
}
