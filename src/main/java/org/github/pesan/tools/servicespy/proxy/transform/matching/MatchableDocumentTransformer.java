package org.github.pesan.tools.servicespy.proxy.transform.matching;

import org.github.pesan.tools.servicespy.proxy.transform.DocumentTransformer;

import java.util.Map;
import java.util.function.Predicate;

public class MatchableDocumentTransformer<U> implements DocumentTransformer<U>, Predicate<String> {
    private final Predicate<String> predicate;
    private final DocumentTransformer<U> transformer;

    public MatchableDocumentTransformer(Predicate<String> predicate, DocumentTransformer<U> transformer) {
        this.predicate = predicate;
        this.transformer = transformer;
    }

    @Override
    public boolean test(String s) {
        return predicate.test(s);
    }

    @Override
    public U transform(U document, Map<String, Object> context) {
        return transformer.transform(document, context);
    }
}
