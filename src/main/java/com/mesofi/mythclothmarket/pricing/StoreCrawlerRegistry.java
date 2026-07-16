package com.mesofi.mythclothmarket.pricing;

import java.util.List;

import org.springframework.stereotype.Component;

import com.mesofi.mythclothmarket.crawler.StoreCrawler;
import com.mesofi.mythclothmarket.crawler.model.StoreName;

import lombok.RequiredArgsConstructor;

/**
 * Registry that provides access to the {@link StoreCrawler} implementation
 * associated with a specific {@link StoreName}.
 * <p>
 * All {@link StoreCrawler} beans are discovered and injected by Spring during
 * application startup. This registry is responsible for resolving the
 * appropriate crawler for a given store, allowing callers to remain independent
 * of the concrete crawler implementations.
 */
@Component
@RequiredArgsConstructor
public class StoreCrawlerRegistry {

    private final List<StoreCrawler> crawlers;

    /**
     * Returns the {@link StoreCrawler} responsible for crawling the specified
     * store.
     *
     * @param store
     *            the store whose crawler should be returned
     * @return the crawler associated with the specified store
     * @throws IllegalArgumentException
     *             if no crawler is registered for the given store
     */
    public StoreCrawler get(StoreName store) {
        return crawlers.stream().filter(crawler -> crawler.store() == store).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No crawler found for store: " + store));
    }
}
