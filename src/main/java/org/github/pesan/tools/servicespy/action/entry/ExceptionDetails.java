package org.github.pesan.tools.servicespy.action.entry;

public class ExceptionDetails {

    private final String name;
    private final String message;
    private final StackTraceElement[] stackTrace;

    public static ExceptionDetails fromThrowable(Throwable throwable) {
        return new ExceptionDetails(throwable.getClass().getName(), throwable.getMessage(), throwable.getStackTrace());
    }

    private ExceptionDetails(String name, String message, StackTraceElement[] stackTrace) {
        this.name = name;
        this.message = message;
        this.stackTrace = stackTrace;
    }

    public String getName() {
        return name;
    }

    public String getMessage() {
        return message;
    }

    public StackTraceElement[] getStackTrace() {
        return stackTrace;
    }
}
