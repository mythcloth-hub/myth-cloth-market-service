package com.mesofi.mythclothmarket.crawler.impl.jp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import java.util.Currency;

import org.junit.jupiter.api.Test;

import com.mesofi.mythclothmarket.crawler.fetcher.PageFetcher;
import com.mesofi.mythclothmarket.crawler.mapper.CrawlerMapper;
import com.mesofi.mythclothmarket.crawler.model.LineUpType;
import com.mesofi.mythclothmarket.crawler.model.ListingStatus;

class NinNinGameStoreCrawlerTest {

    private final NinNinGameStoreCrawler crawler = new NinNinGameStoreCrawler(mock(PageFetcher.class),
            mock(CrawlerMapper.class));

    @Test
    void determineLineUpHandlesDashSeparator() {
        var detection = crawler.determineLineUp("myth cloth ex - gemini saga");

        assertEquals(LineUpType.MYTH_CLOTH_EX, detection.lineUp());
        assertEquals("gemini saga", detection.normalizedName());
    }

    @Test
    void determineLineUpFallsBackToDefaultMatchingWhenNoSeparatorExists() {
        var detection = crawler.determineLineUp("myth cloth ex gemini saga");

        assertEquals(LineUpType.MYTH_CLOTH_EX, detection.lineUp());
        assertEquals("gemini saga", detection.normalizedName());
    }

    @Test
    void determineCurrencyMapsSupportedPrefixes() {
        assertEquals(Currency.getInstance("MXN"), crawler.determineCurrency("MEX 1,999"));
        assertEquals(Currency.getInstance("USD"), crawler.determineCurrency("US 120"));
        assertEquals(Currency.getInstance("EUR"), crawler.determineCurrency("EUR 120"));
        assertEquals(Currency.getInstance("JPY"), crawler.determineCurrency("JPY 120"));
        assertNull(crawler.determineCurrency("ABC 120"));
        assertNull(crawler.determineCurrency(""));
    }

    @Test
    void calculateListingStatusMapsKnownValuesAndDefaultsToDiscontinued() {
        assertEquals(ListingStatus.IN_STOCK, crawler.calculateListingStatus("add to cart"));
        assertEquals(ListingStatus.OUT_OF_STOCK, crawler.calculateListingStatus("soon available"));
        assertEquals(ListingStatus.DISCONTINUED, crawler.calculateListingStatus("sold out"));
        assertNull(crawler.calculateListingStatus(" "));
    }
}
