package com.mesofi.mythclothmarket.pricing;

import java.util.List;

import org.springframework.stereotype.Service;

import com.mesofi.mythclothmarket.crawler.StoreCrawler;
import com.mesofi.mythclothmarket.crawler.model.StoreListing;

import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for synchronizing market pricing information from an
 * online retailer.
 * <p>
 * The synchronization process delegates the retrieval of product listings to
 * the corresponding {@link StoreCrawler} implementation and publishes each
 * retrieved listing for downstream processing. This service is used by both
 * scheduled jobs and manual synchronization endpoints.
 */
@Slf4j
@Service
public class MarketPricingService {

    /**
     * Retrieves the latest product listings from the specified store and publishes
     * each listing for further processing.
     * <p>
     * The supplied {@link StoreCrawler} encapsulates the crawling logic for a
     * specific retailer, including navigation and extraction of product data.
     *
     * @param storeCrawler
     *            the crawler responsible for retrieving listings from a specific
     *            online store
     */
    public void synchronizeStoreListings(StoreCrawler storeCrawler) {
        log.info("Retrieve and publish prices for store: {}", storeCrawler.store());

        // retrieves the prices ...
        List<StoreListing> storeListings = storeCrawler.crawlListings();
        for (StoreListing storeListing : storeListings) {
            // publishes each listing to the message broker ...
            log.info("Publishing listing: {}", storeListing);
        }

        log.info("{} figurines were published", storeListings.size());
    }
}
