package com.mesofi.mythclothmarket.pricing;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.ServletException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.mesofi.mythclothmarket.crawler.StoreCrawler;
import com.mesofi.mythclothmarket.crawler.model.StoreName;

@WebMvcTest(value = MarketPricingController.class)
class MarketPricingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StoreCrawlerRegistry storeCrawlerRegistry;
    @MockitoBean
    private MarketPricingService marketPricingService;

    @Test
    void triggerMarketCrawlShouldReturn202AndTriggerSynchronization() throws Exception {
        // Arrange
        StoreCrawler crawler = mock(StoreCrawler.class);
        when(storeCrawlerRegistry.get(StoreName.LUNA_PARK)).thenReturn(crawler);

        // Act & Assert
        mockMvc.perform(post("/markets/LUNA_PARK/sync")).andExpect(status().isAccepted());

        verify(storeCrawlerRegistry).get(StoreName.LUNA_PARK);
        verify(marketPricingService).synchronizeStoreListings(crawler);
    }

    @Test
    void triggerMarketCrawlShouldReturn500WhenSynchronizationFails() throws Exception {
        // Arrange
        StoreCrawler crawler = mock(StoreCrawler.class);
        when(storeCrawlerRegistry.get(StoreName.NIN_NIN_GAME)).thenReturn(crawler);
        doThrow(new RuntimeException("sync failed")).when(marketPricingService).synchronizeStoreListings(crawler);

        // Act & Assert
        assertThrows(ServletException.class, () -> mockMvc.perform(post("/markets/NIN_NIN_GAME/sync")));
        verify(storeCrawlerRegistry).get(StoreName.NIN_NIN_GAME);
        verify(marketPricingService).synchronizeStoreListings(crawler);
    }

    @Test
    void triggerMarketCrawlShouldReturn400WhenStoreNameIsInvalid() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/markets/UNKNOWN_STORE/sync")).andExpect(status().isBadRequest());
    }
}
