package org.github.pesan.tools.servicespy.proxy;

import org.github.pesan.tools.servicespy.proxy.transform.StreamTransformer;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

@Component
public class ProxyService {
    public ProxyResponse doProxy(String requestPath, StreamTransformer requestTransformer, StreamTransformer responseTransformer, ProxyRequest proxyRequest) throws IOException {
        Map<String, Object> context = new HashMap<>();
        context.put("requestPath", requestPath);
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            CloseableHttpResponse backendResponse = doBackendRequest(client, proxyRequest, context, requestTransformer);
            HttpEntity entity = backendResponse.getEntity();

            responseTransformer.transform(entity.getContent(), proxyRequest.getOutputStream(), context);

            EntityUtils.consume(entity);

            return new ProxyResponse(
                    backendResponse.getStatusLine().getStatusCode(),
                    -1,
                    entity.getContentType() != null ? entity.getContentType().getValue() : null
            );
        }
    }

    private CloseableHttpResponse doBackendRequest(CloseableHttpClient client, ProxyRequest proxyRequest, Map<String, Object> context, StreamTransformer requestTransformer) throws IOException {
        return client.execute(
            RequestBuilder
                    .create(proxyRequest.getHttpMethod())
                    .setUri(proxyRequest.getBackendUrl())
                    .setEntity(getEntityStream(proxyRequest, context, requestTransformer))
                    .build()
        );
    }

    private HttpEntity getEntityStream(ProxyRequest proxyRequest, Map<String, Object> context, StreamTransformer requestTransformer) {
        return new TransformingHttpEntity(requestTransformer, proxyRequest.getInputStream(), proxyRequest.getContentType(), context);
    }

    private class TransformingHttpEntity extends AbstractHttpEntity {
        private final InputStream input;
        private final StreamTransformer transformer;
        private final Map<String, Object> context;

        public TransformingHttpEntity(StreamTransformer transformer, InputStream input, String contentType, Map<String, Object> context) {
            this.input = input;
            this.transformer = transformer;
            this.context = context;
            setContentType(contentType);
        }

        @Override
        public void writeTo(OutputStream outputStream) throws IOException {
            transformer.transform(input, outputStream, context);
        }

        @Override public boolean isRepeatable() { return false; }
        @Override public long getContentLength() { return -1; }
        @Override public InputStream getContent() throws IOException, UnsupportedOperationException { return null; }
        @Override public boolean isStreaming() { return true; }
    }
}
