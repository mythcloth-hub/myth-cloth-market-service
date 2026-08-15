package com.mesofi.mythclothmarket.crawler.impl.mx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import java.util.Currency;

import org.junit.jupiter.api.Test;

import com.mesofi.mythclothmarket.crawler.fetcher.PageFetcher;
import com.mesofi.mythclothmarket.crawler.mapper.CrawlerMapper;
import com.mesofi.mythclothmarket.crawler.model.LineUpType;
import com.mesofi.mythclothmarket.crawler.model.ListingStatus;

class MythSuppliesStoreCrawlerTest {

    private final TestableMythSuppliesStoreCrawler crawler = new TestableMythSuppliesStoreCrawler(
            mock(PageFetcher.class), mock(CrawlerMapper.class));

    @Test
    void determineLineUpUsesBaseAliasMatcherConfiguration() {
        var detection = crawler.resolveLineUp("myth cloth ex milo");

        assertEquals(LineUpType.MYTH_CLOTH_EX, detection.lineUp());
        assertEquals("milo", detection.normalizedName());
    }

    @Test
    void determineLineUpReturnsUnknownWhenNoAliasMatches() {
        var detection = crawler.resolveLineUp("other line item");

        assertNull(detection.lineUp());
        assertEquals("other line item", detection.normalizedName());
    }

    @Test
    void determineCurrencyUsesMxnForAllListings() {
        assertEquals(Currency.getInstance("MXN"), crawler.determineCurrency("whatever"));
    }

    @Test
    void calculateListingStatusMapsAgregarToInStock() {
        assertEquals(ListingStatus.IN_STOCK, crawler.calculateListingStatus("Agregar"));
        assertEquals(ListingStatus.OUT_OF_STOCK, crawler.calculateListingStatus("Not available"));
    }

    @Test
    void prependFlagsEnableBaseUrlForProductAndImage() {
        assertEquals(true, crawler.prependedStoreBaseUrlInProductUrl());
        assertEquals(true, crawler.prependedStoreBaseUrlInImageUrl());
    }

    private static final class TestableMythSuppliesStoreCrawler extends MythSuppliesStoreCrawler {
        private TestableMythSuppliesStoreCrawler(PageFetcher pageFetcher, CrawlerMapper mapper) {
            super(pageFetcher, mapper);
        }

        private com.mesofi.mythclothmarket.crawler.model.LineUpDetection resolveLineUp(String nameText) {
            return determineLineUp(nameText);
        }
    }
}
