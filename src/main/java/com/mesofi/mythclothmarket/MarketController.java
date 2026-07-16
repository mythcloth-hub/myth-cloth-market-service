package com.mesofi.mythclothmarket;

import com.mesofi.mythclothmarket.crawler.StoreCrawler;
import com.mesofi.mythclothmarket.crawler.model.StoreListing;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class MarketController {

    private final MarketService marketService;

    @PostMapping
    public void retrieveAndPublishPrices(StoreCrawler storeCrawler) {
        log.info("Retrieve and publish prices for store: {}", storeCrawler.store());

        // retrieves the prices ...
        List<StoreListing> storeListings = storeCrawler.crawlListings();
        for (StoreListing storeListing : storeListings) {
            // publishes each listing to the message broker ...
            log.debug("Publishing listing: {}", storeListing);
        }
        log.info("{} figurines were published", storeListings.size());
    }
}
