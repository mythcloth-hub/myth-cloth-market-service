package com.mesofi.mythclothmarket.crawler.model;

/**
 * Represents a selector used to locate an HTML element and, optionally,
 * retrieve the value of one of its attributes.
 * <p>
 * The {@code multiple} flag indicates whether the selector should match and
 * process multiple elements ({@code true}) or only a single element
 * ({@code false}).
 * <p>
 * If an attribute is specified, the crawler extracts the value of that
 * attribute from the matched element (for example, {@code href} or
 * {@code src}). Otherwise, the element's text content is typically used.
 *
 * @param multiple
 *            whether to select multiple matching elements
 * @param selector
 *            the CSS selector used to locate the element
 * @param attribute
 *            the name of the attribute to extract, or {@code null} to use the
 *            element's text content
 */
public record ElementSelector(boolean multiple, String selector, String attribute) {

    /**
     * Creates an {@code ElementSelector} that selects a single element.
     *
     * @param selector
     *            the CSS selector used to locate the element
     * @param attribute
     *            the name of the attribute to extract, or {@code null} to use the
     *            element's text content
     */
    public ElementSelector(String selector, String attribute) {
        this(false, selector, attribute);
    }

    /**
     * Creates an {@code ElementSelector} that extracts the text content of the
     * matched single element.
     *
     * @param selector
     *            the CSS selector used to locate the element
     */
    public ElementSelector(String selector) {
        this(false, selector, null);
    }

    /**
     * Creates an {@code ElementSelector} that extracts the text content of matched
     * element(s).
     *
     * @param multiple
     *            whether to select multiple matching elements
     * @param selector
     *            the CSS selector used to locate the element
     */
    public ElementSelector(boolean multiple, String selector) {
        this(multiple, selector, null);
    }
}
