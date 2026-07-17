package com.mesofi.mythclothmarket.crawler.model;

/**
 * Represents a selector used to locate an HTML element and, optionally,
 * retrieve the value of one of its attributes.
 * <p>
 * If an attribute is specified, the crawler extracts the value of that
 * attribute from the matched element (for example, {@code href} or
 * {@code src}). Otherwise, the element's text content is typically used.
 *
 * @param selector
 *            the CSS selector used to locate the element
 * @param attribute
 *            the name of the attribute to extract, or {@code null} to use the
 *            element's text content
 */
public record ElementSelector(String selector, String attribute) {

    /**
     * Creates an {@code ElementSelector} that extracts the text content of the
     * matched element.
     *
     * @param selector
     *            the CSS selector used to locate the element
     */
    public ElementSelector(String selector) {
        this(selector, null);
    }
}
