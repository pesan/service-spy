package org.github.pesan.tools.servicespy.action.entry;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.github.pesan.tools.servicespy.proxy.RequestContext;

public class RequestDataEntry implements RequestEntry {
    private final String requestPath;
    private final String requestPathWithQuery;
    private final String httpMethod;
    private final Map<String, List<String>> headers;
    private final LocalDateTime time;
    private final Exception exception;

    private final ByteArrayOutputStream data;

    public static RequestDataEntry fromContext(RequestContext context, ByteArrayOutputStream received) {
    	return new RequestDataEntry(context.getRequestUri(), context.getRequestMethod(), context.getRequestHeaders(), received, context.getStartTime(), null);
    }

    public static RequestDataEntry fromContext(RequestContext context, Exception exception) {
    	return new RequestDataEntry(context.getRequestUri(), context.getRequestMethod(), context.getRequestHeaders(), new ByteArrayOutputStream(), context.getStartTime(), exception);
    }

    public RequestDataEntry(URI requestUri, String httpMethod, Map<String, List<String>> headers, ByteArrayOutputStream data, LocalDateTime time, Exception exception) {
        this.requestPath = requestUri.getPath();
        this.requestPathWithQuery = requestUri.getPath() + (requestUri.getQuery() != null ? "?" + requestUri.getQuery() : "");
        this.httpMethod = httpMethod;
        this.headers = headers;
        this.data = data;
        this.time = time;
        this.exception = exception;
    }

    @Override
	public String getRequestPath() {
        return requestPath;
    }

    @Override
	public String getRequestPathWithQuery() {
        return requestPathWithQuery;
    }

    @Override
	public String getHttpMethod() {
        return httpMethod;
    }

    @Override
	public Map<String, List<String>> getHeaders() {
        return headers;
    }

    @Override
	public byte[] getData() {
        return data.toByteArray();
    }

    @Override
	public LocalDateTime getTime() {
        return time;
    }

	public Exception getException() {
		return exception;
	}
}
