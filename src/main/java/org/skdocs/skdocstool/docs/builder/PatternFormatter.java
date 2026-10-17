package org.skdocs.skdocstool.docs.builder;

public final class PatternFormatter {
    private PatternFormatter() {}
    static String format(String pattern) {
        return pattern.replaceAll("\\b\\d+¦", "")
            .replaceAll("\\b[\\w-]+:(?=[\\[(])", "")
            .replaceAll("%[-~]", "%");
    }
}
