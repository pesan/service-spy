package org.github.pesan.tools.servicespy.dashboard.entry;

import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class ExceptionDetails {

    public static class StackFrame {
        private final int lineNumber;
        private final String className;
        private final String methodName;
        private final String fileName;

        public StackFrame(int lineNumber, String className, String methodName, String fileName) {
            this.lineNumber = lineNumber;
            this.className = className;
            this.methodName = methodName;
            this.fileName = fileName;
        }

        public static StackFrame fromStackTraceElement(StackTraceElement element) {
            return new StackFrame(
                    element.getLineNumber(),
                    element.getClassName(),
                    element.getMethodName(),
                    element.getFileName()
            );
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public String getClassName() {
            return className;
        }

        public String getMethodName() {
            return methodName;
        }

        public String getFileName() {
            return fileName;
        }
    }

    private final String name;
    private final String message;
    private final List<StackFrame> stackTrace;

    public static ExceptionDetails fromThrowable(Throwable throwable) {
        return new ExceptionDetails(
                throwable.getClass().getName(),
                throwable.getMessage(),
                Arrays.stream(throwable.getStackTrace().length != 0 || throwable.getCause() == null
                        ? throwable.getStackTrace() : throwable.getCause().getStackTrace()
                ).map(StackFrame::fromStackTraceElement).collect(toList()));
    }

    public ExceptionDetails(String name, String message, List<StackFrame> stackTrace) {
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

    public List<StackFrame> getStackTrace() {
        return stackTrace;
    }
}