package org.github.pesan.tools.servicespy.proxy.transform.matching;

import org.github.pesan.tools.servicespy.proxy.transform.DocumentTransformer;
import org.github.pesan.tools.servicespy.proxy.transform.StreamTransformer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MatchingTransformer<U> implements StreamTransformer {
    public static Predicate<String> matches(String pattern) {
        return matches(Pattern.compile(pattern));
    }

    public static Predicate<String> matches(Pattern pattern) {
        return x -> pattern.matcher(x).matches();
    }

    public static Predicate<String> equalTo(String serviceName) {
        return serviceName::equals;
    }

    private final List<MatchableDocumentTransformer<U>> transformers;
    private final DocumentSerializer<U> serializer;
    private final DocumentDeserializer<U> deserializer;

    public MatchingTransformer(List<MatchableDocumentTransformer<U>> transformers, DocumentSerializer<U> serializer, DocumentDeserializer<U> deserializer) {
        this.transformers = transformers;
        this.serializer = serializer;
        this.deserializer = deserializer;
    }

    @Override
    public void transform(InputStream in, OutputStream out, Map<String, Object> context) throws IOException {
        String requestPath = (String)context.get("requestPath");
        List<MatchableDocumentTransformer<U>> ts = transformers.stream()
                .filter(t -> t.test(requestPath))
                .collect(Collectors.toList());
        if (!ts.isEmpty()) {
            U document = deserializer.deserialize(in);
            for (DocumentTransformer<U> t : ts) {
                document = t.transform(document, context);
            }
            serializer.serialize(document, out);
        } else {
            PASS_THROUGH.transform(in, out, context);
        }
    }
}
