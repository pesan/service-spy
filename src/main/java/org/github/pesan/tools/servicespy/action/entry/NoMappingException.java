package org.github.pesan.tools.servicespy.action.entry;

public class NoMappingException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public NoMappingException(String requestPath) {
        super(String.format("No mapping for request path: %s", requestPath));
    }

}
