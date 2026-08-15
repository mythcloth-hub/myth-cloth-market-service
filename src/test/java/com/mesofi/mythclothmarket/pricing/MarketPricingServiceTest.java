package com.mesofi.mythclothmarket.pricing;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.mesofi.mythclothmarket.crawler.StoreCrawler;
import com.mesofi.mythclothmarket.crawler.model.LineUpType;
import com.mesofi.mythclothmarket.crawler.model.ListingStatus;
import com.mesofi.mythclothmarket.crawler.model.StoreListing;
import com.mesofi.mythclothmarket.crawler.model.StoreName;
import com.mesofi.mythclothmarket.messaging.MessagePublisher;

@SpringBootTest(classes = {MarketPricingService.class})
class MarketPricingServiceTest {

    @Autowired
    private MarketPricingService marketPricingService;

    @MockitoBean
    private MessagePublisher messagePublisher;

    @Test
    void synchronizeStoreListingsShouldPublishAllListingsWhenAllMessagesAreAccepted() {
        // Arrange
        StoreCrawler crawler = org.mockito.Mockito.mock(StoreCrawler.class);
        when(crawler.store()).thenReturn(StoreName.LUNA_PARK);
        when(crawler.crawlListings())
                .thenReturn(List.of(createListing("https://store/a"), createListing("https://store/b")));
        when(messagePublisher.publishCrawlerMessage(any(StoreListing.class))).thenReturn(true);

        // Act
        marketPricingService.synchronizeStoreListings(crawler);

        // Verify
        verify(crawler).store();
        verify(crawler).crawlListings();
        verify(messagePublisher, times(2)).publishCrawlerMessage(any(StoreListing.class));
    }

    @Test
    void synchronizeStoreListingsShouldContinueWhenOnePublishFailsWithException() {
        // Arrange
        StoreCrawler crawler = org.mockito.Mockito.mock(StoreCrawler.class);
        when(crawler.store()).thenReturn(StoreName.MYTH_FACTORY);
        when(crawler.crawlListings()).thenReturn(List.of(createListing("https://store/a"),
                createListing("https://store/b"), createListing("https://store/c")));
        when(messagePublisher.publishCrawlerMessage(any(StoreListing.class))).thenReturn(true)
                .thenThrow(new RuntimeException("boom")).thenReturn(true);

        // Act + Assert
        assertDoesNotThrow(() -> marketPricingService.synchronizeStoreListings(crawler));

        // Verify
        verify(crawler).store();
        verify(crawler).crawlListings();
        verify(messagePublisher, times(3)).publishCrawlerMessage(any(StoreListing.class));
    }

    @Test
    void synchronizeStoreListingsShouldHandleSkippedMessagesWhenPublisherReturnsFalse() {
        // Arrange
        StoreCrawler crawler = org.mockito.Mockito.mock(StoreCrawler.class);
        when(crawler.store()).thenReturn(StoreName.NIN_NIN_GAME);
        when(crawler.crawlListings())
                .thenReturn(List.of(createListing("https://store/a"), createListing("https://store/b")));
        when(messagePublisher.publishCrawlerMessage(any(StoreListing.class))).thenReturn(false);

        // Act
        marketPricingService.synchronizeStoreListings(crawler);

        // Verify
        verify(crawler).store();
        verify(crawler).crawlListings();
        verify(messagePublisher, times(2)).publishCrawlerMessage(any(StoreListing.class));
    }

    private StoreListing createListing(String productUrl) {
        return new StoreListing(StoreName.LUNA_PARK, LineUpType.MYTH_CLOTH_EX, "Original Name", "Normalized Name",
                "https://images.test/item.jpg", productUrl, BigDecimal.valueOf(120), null, BigDecimal.valueOf(120),
                Currency.getInstance("JPY"), ListingStatus.IN_STOCK, false, Instant.parse("2024-06-01T12:00:00Z"));
    }
}
