package com.mesofi.mythclothmarket.crawler.model;

/**
 * Defines the CSS selectors required to crawl product listings from a store
 * page.
 * <p>
 * This record groups the selectors used to locate the product listing
 * container, pagination controls, and the individual elements that provide
 * product information such as the name, image, URL, price, discount, and
 * availability.
 *
 * @param listingContainer
 *            the CSS selector identifying each product listing on the page
 * @param nextPage
 *            the CSS selector for the control used to navigate to the next page
 * @param productName
 *            the selector used to extract the product name
 * @param productImage
 *            the selector used to extract the product image URL
 * @param productUrl
 *            the selector used to extract the product detail page URL
 * @param productPrice
 *            the selector used to extract the product price
 * @param discount
 *            the selector used to extract any discount information
 * @param availability
 *            the selector used to extract the product availability or stock
 *            status
 */
public record StorePageSelectors(String listingContainer, String nextPage, ElementSelector productName,
        ElementSelector productImage, ElementSelector productUrl, ElementSelector productPrice,
        ElementSelector discount, ElementSelector availability) {
}
