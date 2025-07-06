package com.mememan.nexus.util;

/**
 * Generic utility {@code class} that provides additional methods for convenient {@link String manipulation} beyond what
 * is provided by both Java's standard library and other included libraries (such as Guava and Apache Commons).
 */
public final class StringUtil {

    private StringUtil() {
        throw new IllegalAccessError("Attempted to construct instance of utility class! (StringUtil)");
    }

    /**
     * Capitalizes a {@link String} formatted in snake case into a title case {@link String},
     * e.g. {@code "some_lower_case_string"} -> {@code "Some Lower Case String"}.
     *
     * @param targetString the {@link String} to capitalize.
     *
     * @return The capitalized {@link String}
     */
    public static String toTitleCase(String targetString) {
        if (targetString == null || targetString.isEmpty()) return targetString;

        StringBuilder titleCaseBuilder = new StringBuilder();
        boolean capitalizeNext = true;

        for (char curChar : targetString.toCharArray()) {
            if (capitalizeNext) {
                titleCaseBuilder.append(Character.toTitleCase(curChar));
                capitalizeNext = false;
            } else titleCaseBuilder.append(Character.toLowerCase(curChar));

            if (curChar == '_') capitalizeNext = true;
        }

        return titleCaseBuilder.toString();
    }
}
