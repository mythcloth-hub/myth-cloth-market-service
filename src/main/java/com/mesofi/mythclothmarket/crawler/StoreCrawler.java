package com.mesofi.mythclothmarket.crawler;

import java.util.List;

import com.mesofi.mythclothmarket.crawler.model.StoreListing;
import com.mesofi.mythclothmarket.crawler.model.StoreName;

/**
 * Contract for crawling listings from a specific store.
 */
public interface StoreCrawler {
    /**
     * @return store identifier for the crawler implementation.
     */
    StoreName store();

    /**
     * Crawls the store and returns normalized listings.
     *
     * @return list of crawled listings.
     */
    List<StoreListing> crawlListings();
}
