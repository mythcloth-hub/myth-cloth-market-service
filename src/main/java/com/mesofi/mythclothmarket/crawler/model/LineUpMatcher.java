package com.mesofi.mythclothmarket.crawler.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Associates a {@link LineUp} with the compiled pattern used to recognize it in
 * a product name.
 *
 * @param lineUp
 *            the lineup represented by this matcher
 * @param pattern
 *            the compiled pattern used to identify the lineup
 */
public record LineUpMatcher(LineUp lineUp, Pattern pattern) {

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
     * Removes the matched lineup prefix from the supplied product name and returns
     * the remaining character or product name.
     *
     * @param text
     *            the complete product name
     * @return the product name without the lineup prefix, or {@code null} if this
     *         matcher does not recognize the supplied text
     */
    public String extractProductName(String text) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return text.substring(matcher.end()).trim();
        }
        return null;
    }
}
