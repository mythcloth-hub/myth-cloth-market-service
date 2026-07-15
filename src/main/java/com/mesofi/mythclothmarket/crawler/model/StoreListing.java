package com.mesofi.mythclothmarket.crawler.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;

public record StoreListing(StoreName store, String productName, String lineUp, BigDecimal price, BigDecimal discount,
        BigDecimal discountedPrice, Currency currency, String productUrl, ListingStatus status, Instant checkedAt) {
}
