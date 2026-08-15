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

class MyKombiniStoreCrawlerTest {

    private final MyKombiniStoreCrawler crawler = new MyKombiniStoreCrawler(mock(PageFetcher.class),
            mock(CrawlerMapper.class));

    @Test
    void determineLineUpResolvesKnownAliases() {
        var detection = crawler.determineLineUp("myth cloth ex gemini saga");

        assertEquals(LineUpType.MYTH_CLOTH_EX, detection.lineUp());
        assertEquals("gemini saga", detection.normalizedName());
    }

    @Test
    void determineLineUpReturnsUnknownWhenNoAliasMatches() {
        var detection = crawler.determineLineUp("random product");

        assertNull(detection.lineUp());
        assertEquals("random product", detection.normalizedName());
    }

    @Test
    void calculateListingStatusMapsAddToCartToInStock() {
        assertEquals(ListingStatus.IN_STOCK, crawler.calculateListingStatus("Add to cart"));
        assertEquals(ListingStatus.OUT_OF_STOCK, crawler.calculateListingStatus("Sold out"));
    }

    @Test
    void removeUnnecessaryWordsCleansBrandingNoise() {
        assertEquals("myth cloth ex", crawler.removeUnnecessaryWords("Bandai Spirits Saint Seiya myth cloth ex"));
    }

    @Test
    void determineCurrencyUsesYenForAllListings() {
        assertEquals(Currency.getInstance("JPY"), crawler.determineCurrency("anything"));
    }
}
