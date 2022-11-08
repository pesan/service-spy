package org.github.pesan.tools.servicespy.application;

record StackFrame(int lineNumber,
                  String className,
                  String methodName,
                  String fileName) {
}
