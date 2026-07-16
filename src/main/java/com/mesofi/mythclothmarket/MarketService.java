package com.mesofi.mythclothmarket;

import java.util.List;

import org.springframework.stereotype.Service;

import com.mesofi.mythclothmarket.crawler.StoreCrawler;
import com.mesofi.mythclothmarket.crawler.model.StoreListing;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MarketService {

    public void retrieveAndPublishPrices(StoreCrawler storeCrawler) {
        log.info("Retrieve and publish prices for store: {}", storeCrawler.store());

        // retrieves the prices ...
        List<StoreListing> storeListings = storeCrawler.crawlListings();
        for (StoreListing storeListing : storeListings) {
            // publishes each listing to the message broker ...
            log.info("Publishing listing: {}", storeListing);
        }
    }
}
