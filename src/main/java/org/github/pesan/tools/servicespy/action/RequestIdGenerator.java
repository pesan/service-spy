package org.github.pesan.tools.servicespy.action;

@FunctionalInterface
public interface RequestIdGenerator {
    String next();
}
