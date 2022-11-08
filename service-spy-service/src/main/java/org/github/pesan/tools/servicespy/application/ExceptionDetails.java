package org.github.pesan.tools.servicespy.application;

import java.util.List;

public record ExceptionDetails(String name,
                               String message,
                               List<StackFrame> stackTrace) {
}
