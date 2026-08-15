package com.mesofi.mythclothmarket.crawler.impl.be;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import java.util.Currency;

import org.junit.jupiter.api.Test;

import com.mesofi.mythclothmarket.crawler.fetcher.PageFetcher;
import com.mesofi.mythclothmarket.crawler.mapper.CrawlerMapper;
import com.mesofi.mythclothmarket.crawler.model.LineUpType;
import com.mesofi.mythclothmarket.crawler.model.ListingStatus;

class MythFactoryStoreCrawlerTest {

    private final TestableMythFactoryStoreCrawler crawler = new TestableMythFactoryStoreCrawler(mock(PageFetcher.class),
            mock(CrawlerMapper.class));

    @Test
    void determineLineUpUsesBaseAliasMatcherConfiguration() {
        var detection = crawler.resolveLineUp("saint cloth myth ex gemini saga");

        assertEquals(LineUpType.MYTH_CLOTH_EX, detection.lineUp());
        assertEquals("gemini saga", detection.normalizedName());
    }

    @Test
    void determineLineUpReturnsUnknownWhenNoAliasMatches() {
        var detection = crawler.resolveLineUp("other product line");

        assertNull(detection.lineUp());
        assertEquals("other product line", detection.normalizedName());
    }

    @Test
    void determineCurrencyUsesEurForAllListings() {
        assertEquals(Currency.getInstance("EUR"), crawler.determineCurrency("anything"));
    }

    @Test
    void calculateListingStatusMapsOutOfStockText() {
        assertEquals(ListingStatus.OUT_OF_STOCK, crawler.calculateListingStatus("Out of stock"));
        assertEquals(ListingStatus.IN_STOCK, crawler.calculateListingStatus("In stock"));
    }

    private static final class TestableMythFactoryStoreCrawler extends MythFactoryStoreCrawler {
        private TestableMythFactoryStoreCrawler(PageFetcher pageFetcher, CrawlerMapper mapper) {
            super(pageFetcher, mapper);
        }

        private com.mesofi.mythclothmarket.crawler.model.LineUpDetection resolveLineUp(String nameText) {
            return determineLineUp(nameText);
        }
    }
}
