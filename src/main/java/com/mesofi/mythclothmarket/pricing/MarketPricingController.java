package com.mesofi.mythclothmarket.pricing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mesofi.mythclothmarket.crawler.model.StoreName;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller that exposes endpoints for manually synchronizing market data
 * from supported online retailers.
 * <p>
 * These endpoints trigger the same crawling workflow used by the scheduled
 * jobs, allowing administrators or developers to initiate a synchronization on
 * demand. During the synchronization process, the corresponding
 * {@link com.mesofi.mythclothmarket.crawler.StoreCrawler} retrieves the latest
 * product listings for the specified store and publishes the resulting data for
 * downstream processing.
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/markets")
@RequiredArgsConstructor
public class MarketPricingController {

    private final StoreCrawlerRegistry storeCrawlerRegistry;
    private final MarketPricingService marketService;

    /**
     * Triggers an on-demand synchronization for the specified market store.
     * <p>
     * The synchronization crawls the store's current listings and publishes the
     * retrieved information for further processing. This endpoint performs the same
     * operation as the scheduled synchronization job but allows it to be invoked
     * manually.
     *
     * @param storeName
     *            the store whose listings should be synchronized
     * @return a {@link ResponseEntity} with HTTP {@code 202 Accepted} indicating
     *         that the synchronization request has been accepted for processing
     */
    @PostMapping("/{storeName}/sync")
    public ResponseEntity<Void> triggerMarketCrawl(@NotNull @Valid @PathVariable StoreName storeName) {
        log.info("Triggering market crawl for store '{}'.", storeName);

        marketService.synchronizeStoreListings(storeCrawlerRegistry.get(storeName));

        log.info("Finished market crawl for store '{}'.", storeName);
        return ResponseEntity.accepted().build();
    }
}
