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

class LoganStoreCrawlerTest {

    private final TestableLoganStoreCrawler crawler = new TestableLoganStoreCrawler(mock(PageFetcher.class),
            mock(CrawlerMapper.class));

    @Test
    void determineLineUpUsesBaseAliasMatcherConfiguration() {
        var detection = crawler.resolveLineUp("myth cloth ex shiryu");

        assertEquals(LineUpType.MYTH_CLOTH_EX, detection.lineUp());
        assertEquals("shiryu", detection.normalizedName());
    }

    @Test
    void determineLineUpReturnsUnknownWhenNoAliasMatches() {
        var detection = crawler.resolveLineUp("no lineup phrase");

        assertNull(detection.lineUp());
        assertEquals("no lineup phrase", detection.normalizedName());
    }

    @Test
    void determineCurrencyAndStatusUseStoreDefaults() {
        assertEquals(Currency.getInstance("MXN"), crawler.determineCurrency("whatever"));
        assertEquals(ListingStatus.IN_STOCK, crawler.calculateListingStatus("any"));
    }

    private static final class TestableLoganStoreCrawler extends LoganStoreCrawler {
        private TestableLoganStoreCrawler(PageFetcher pageFetcher, CrawlerMapper mapper) {
            super(pageFetcher, mapper);
        }

        private com.mesofi.mythclothmarket.crawler.model.LineUpDetection resolveLineUp(String nameText) {
            return determineLineUp(nameText);
        }
    }
}
