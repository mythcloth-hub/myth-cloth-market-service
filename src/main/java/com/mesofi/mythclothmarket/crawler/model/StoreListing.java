package com.mesofi.mythclothmarket.crawler.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;

/**
 * Normalized store listing produced by the crawler pipeline.
 *
 * @param store
 *            source store.
 * @param productName
 *            product title.
 * @param lineUp
 *            optional collection or series identifier.
 * @param price
 *            listed price.
 * @param discount
 *            discount percentage.
 * @param discountedPrice
 *            computed discounted price.
 * @param currency
 *            currency for the listing price.
 * @param productUrl
 *            product page URL.
 * @param status
 *            normalized listing status.
 * @param checkedAt
 *            timestamp when the listing was crawled.
 */
public record StoreListing(StoreName store, String productName, String lineUp, BigDecimal price, BigDecimal discount,
        BigDecimal discountedPrice, Currency currency, String productUrl, ListingStatus status, Instant checkedAt) {
}
