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

class MandarakeStoreCrawlerTest {

    private final MandarakeStoreCrawler crawler = new MandarakeStoreCrawler(mock(PageFetcher.class),
            mock(CrawlerMapper.class));

    @Test
    void determineLineUpResolvesKnownAliases() {
        var detection = crawler.determineLineUp("cloth myth ex aiolos");

        assertEquals(LineUpType.MYTH_CLOTH_EX, detection.lineUp());
        assertEquals("aiolos", detection.normalizedName());
    }

    @Test
    void determineLineUpReturnsUnknownWhenNoAliasMatches() {
        var detection = crawler.determineLineUp("unknown figure");

        assertNull(detection.lineUp());
        assertEquals("unknown figure", detection.normalizedName());
    }

    @Test
    void removeUnnecessaryWordsCleansBrandAndSeriesNoise() {
        assertEquals("myth cloth ex", crawler.removeUnnecessaryWords("Bandai spirits saint seiya myth cloth ex"));
    }

    @Test
    void determineCurrencyAndStatusUseStoreDefaults() {
        assertEquals(Currency.getInstance("JPY"), crawler.determineCurrency("any"));
        assertEquals(ListingStatus.IN_STOCK, crawler.calculateListingStatus("any"));
    }

    @Test
    void prependFlagsMatchMandarakeBehavior() {
        assertEquals(true, crawler.prependedStoreBaseUrlInProductUrl());
        assertEquals(false, crawler.prependedStoreBaseUrlInImageUrl());
    }
}
