package com.mesofi.mythclothmarket.crawler.model;

/**
 * CSS selectors used to crawl listing data for a store page.
 *
 * @param item
 *            selector for each listing item container.
 * @param nextPage
 *            selector for the next-page link.
 * @param productName
 *            selector for listing title/link.
 * @param priceContainer
 *            selector for the price block.
 * @param price
 *            selector for the base price.
 * @param discount
 *            selector for the discount text.
 * @param availability
 *            selector for availability text.
 */
public record StorePageSelectors(String item, String nextPage, String productName, String priceContainer, String price,
        String discount, String availability) {
}
