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

class LunaParkStoreCrawlerTest {

    private final LunaParkStoreCrawler crawler = new LunaParkStoreCrawler(mock(PageFetcher.class),
            mock(CrawlerMapper.class));

    @Test
    void determineLineUpReturnsMythClothExWhenAliasIsPresent() {
        var detection = crawler.determineLineUp("myth cloth ex gemini saga");

        assertEquals(LineUpType.MYTH_CLOTH_EX, detection.lineUp());
        assertEquals("gemini saga", detection.normalizedName());
    }

    @Test
    void determineLineUpReturnsUnknownWhenAliasIsNotPresent() {
        var detection = crawler.determineLineUp("random product");

        assertNull(detection.lineUp());
        assertEquals("random product", detection.normalizedName());
    }

    @Test
    void removeUnnecessaryWordsCleansStoreNoise() {
        assertEquals("myth cloth ex", crawler.removeUnnecessaryWords("Bandai Saint myth cloth ex japan version"));
    }

    @Test
    void filterImageUrlKeepsOnlyJpgSegment() {
        assertEquals("https://cdn.example.com/image.jpg",
                crawler.filterImageUrl("https://cdn.example.com/image.jpg?width=100"));
    }

    @Test
    void determineCurrencyAndStatusUseStoreDefaults() {
        assertEquals(Currency.getInstance("JPY"), crawler.determineCurrency("any"));
        assertEquals(ListingStatus.IN_STOCK, crawler.calculateListingStatus("any"));
    }
}
