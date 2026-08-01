package com.mesofi.mythclothmarket.crawler.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Associates a {@link LineUpType} with the compiled pattern used to recognize
 * it in a product name.
 *
 * @param lineUp
 *            the lineup represented by this matcher
 * @param pattern
 *            the compiled pattern used to identify the lineup
 */
public record LineUpMatcher(LineUpType lineUp, Pattern pattern) {

    /**
     * Determines whether the specified text matches this lineup.
     *
     * @param text
     *            the product name prefix to test
     * @return {@code true} if the text matches this lineup; {@code false} otherwise
     */
    public boolean matches(String text) {
        return pattern.matcher(text).find();
    }

    /**
     * Removes the substring matched by this extractor's pattern from the supplied
     * product name and returns the remaining text.
     *
     * @param text
     *            the complete product name
     * @return the product name with the matched substring removed, or {@code null}
     *         if the supplied text does not match this extractor's pattern
     */
    public String extractProductName(String text) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return (text.substring(0, matcher.start()).trim() + " " + text.substring(matcher.end()).trim()).trim();
        }
        return null;
    }
}
