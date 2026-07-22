package com.mesofi.mythclothmarket.utils;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility methods for creating commonly used regular expression patterns.
 * <p>
 * This class centralizes the construction of reusable {@link Pattern} instances
 * used throughout the crawler to simplify text matching and normalization.
 * </p>
 */
public final class RegexUtils {

    private RegexUtils() {
        // Utility class
    }

    /**
     * Compiles a case-insensitive regular expression that matches any of the
     * supplied aliases as complete words.
     * <p>
     * Each alias is escaped using {@link Pattern#quote(String)} so that any regular
     * expression metacharacters are treated literally. Word boundaries are added to
     * prevent partial matches, such as matching {@code EX} within a longer word.
     * </p>
     *
     * @param aliases
     *            the aliases to include in the pattern
     * @return a compiled {@link Pattern} that matches any of the supplied aliases
     */
    public static Pattern compileAliases(String... aliases) {
        return Pattern.compile(
                "\\b(?:" + Arrays.stream(aliases).map(Pattern::quote).collect(Collectors.joining("|")) + ")\\b",
                Pattern.CASE_INSENSITIVE);
    }
}
