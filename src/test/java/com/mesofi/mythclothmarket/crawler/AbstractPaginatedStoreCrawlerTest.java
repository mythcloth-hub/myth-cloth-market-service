package com.mesofi.mythclothmarket.crawler;

import static com.mesofi.mythclothmarket.utils.RegexUtils.compileAliases;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.Currency;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.mesofi.mythclothmarket.crawler.fetcher.PageFetcher;
import com.mesofi.mythclothmarket.crawler.mapper.CrawlerMapper;
import com.mesofi.mythclothmarket.crawler.mapper.RawStoreListing;
import com.mesofi.mythclothmarket.crawler.model.ElementSelector;
import com.mesofi.mythclothmarket.crawler.model.LineUpMatcher;
import com.mesofi.mythclothmarket.crawler.model.LineUpType;
import com.mesofi.mythclothmarket.crawler.model.ListingStatus;
import com.mesofi.mythclothmarket.crawler.model.StoreListing;
import com.mesofi.mythclothmarket.crawler.model.StoreName;
import com.mesofi.mythclothmarket.crawler.model.StorePageSelectors;

class AbstractPaginatedStoreCrawlerTest {

    @Test
    void crawlListingsThrowsWhenMaxPagesIsZero() {
        PageFetcher fetcher = mock(PageFetcher.class);
        CrawlerMapper mapper = mock(CrawlerMapper.class);
        AbstractPaginatedStoreCrawler crawler = new TestCrawler(fetcher, mapper, 0);

        assertThrows(IllegalArgumentException.class, crawler::crawlListings);
    }

    @Test
    void crawlListingsNormalizesFieldsAndTraversesPagination() {
        PageFetcher fetcher = mock(PageFetcher.class);
        CrawlerMapper mapper = mock(CrawlerMapper.class);
        AbstractPaginatedStoreCrawler crawler = new TestCrawler(fetcher, mapper, 2);

        String firstPage = """
                <html><body>
                    <article class='item'>
                        <h2 class='name'>Myth Cloth EX Hero</h2>
                        <img class='image' src='/img/hero.jpg?size=large'/>
                        <a class='url' href='/p/hero'>detail</a>
                        <span class='price'>100</span>
                        <span class='availability'>available</span>
                    </article>
                    <a class='next' href='/search?page=2'>next</a>
                </body></html>
                """;
        String secondPage = """
                <html><body>
                    <article class='item'>
                        <h2 class='name'>Myth Cloth Phoenix</h2>
                        <img class='image' src='/img/phoenix.jpg?size=small'/>
                        <a class='url' href='/p/phoenix'>detail</a>
                        <span class='price'>200</span>
                        <span class='availability'>available</span>
                    </article>
                </body></html>
                """;

        when(fetcher.fetch(URI.create("https://store.test/search"))).thenReturn(firstPage);
        when(fetcher.fetch(URI.create("https://store.test/search?page=2"))).thenReturn(secondPage);
        when(mapper.toStoreListing(any(), eq(StoreName.LUNA_PARK), any(), any(), any(), any()))
                .thenReturn(new StoreListing(StoreName.LUNA_PARK, LineUpType.MYTH_CLOTH_EX, "o", "n", "i", "u",
                        BigDecimal.ONE, null, BigDecimal.ONE, Currency.getInstance("JPY"), ListingStatus.IN_STOCK,
                        false, Instant.now()))
                .thenReturn(new StoreListing(StoreName.LUNA_PARK, LineUpType.MYTH_CLOTH, "o2", "n2", "i2", "u2",
                        BigDecimal.TWO, null, BigDecimal.TWO, Currency.getInstance("JPY"), ListingStatus.IN_STOCK,
                        false, Instant.now()));

        List<StoreListing> listings = crawler.crawlListings();

        assertEquals(2, listings.size());

        ArgumentCaptor<RawStoreListing> rawCaptor = ArgumentCaptor.forClass(RawStoreListing.class);
        verify(mapper).toStoreListing(rawCaptor.capture(), eq(StoreName.LUNA_PARK), eq(LineUpType.MYTH_CLOTH_EX), any(),
                any(), any());
        RawStoreListing firstCaptured = rawCaptor.getValue();
        assertEquals("hero", firstCaptured.getNormalizedName());
        assertEquals("https://store.test/p/hero", firstCaptured.getProductUrl());
        assertEquals("https://store.test/img/hero.jpg", firstCaptured.getImageUrl());
    }

    private static final class TestCrawler extends AbstractPaginatedStoreCrawler {
        private final int maxPages;

        private TestCrawler(PageFetcher pageFetcher, CrawlerMapper mapper, int maxPages) {
            super(pageFetcher, mapper);
            this.maxPages = maxPages;
        }

        @Override
        public StoreName store() {
            return StoreName.LUNA_PARK;
        }

        @Override
        protected URI storeBaseUrl() {
            return URI.create("https://store.test");
        }

        @Override
        protected String getInitialSearchUrl() {
            return "/search";
        }

        @Override
        protected int getMaxPages() {
            return maxPages;
        }

        @Override
        protected StorePageSelectors selectors() {
            return new StorePageSelectors("article.item", "a.next", new ElementSelector("h2.name"),
                    new ElementSelector("img.image", "src"), new ElementSelector("a.url", "href"),
                    new ElementSelector("span.price"), null, new ElementSelector("span.availability"), null);
        }

        @Override
        protected List<LineUpMatcher> getLineUpMatchers() {
            return List.of(new LineUpMatcher(LineUpType.MYTH_CLOTH_EX, compileAliases("myth cloth ex")),
                    new LineUpMatcher(LineUpType.MYTH_CLOTH, compileAliases("myth cloth")));
        }

        @Override
        protected Currency determineCurrency(String priceText) {
            return Currency.getInstance("JPY");
        }

        @Override
        protected ListingStatus calculateListingStatus(String availabilityText) {
            return ListingStatus.IN_STOCK;
        }

        @Override
        protected boolean prependedStoreBaseUrlInProductUrl() {
            return true;
        }

        @Override
        protected boolean prependedStoreBaseUrlInImageUrl() {
            return true;
        }

        @Override
        protected String filterImageUrl(String imageUrl) {
            int separator = imageUrl.indexOf('?');
            return separator >= 0 ? imageUrl.substring(0, separator) : imageUrl;
        }

        @Override
        protected String removeUnnecessaryWords(String nameText) {
            return nameText.replace("figure", "").replaceAll("\\s+", " ").trim();
        }
    }
}
