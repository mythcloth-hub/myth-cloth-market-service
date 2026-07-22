package com.mesofi.mythclothmarket.crawler.impl;

import java.util.Currency;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.mesofi.mythclothmarket.crawler.AbstractPaginatedStoreCrawler;
import com.mesofi.mythclothmarket.crawler.fetcher.PageFetcher;
import com.mesofi.mythclothmarket.crawler.mapper.CrawlerMapper;
import com.mesofi.mythclothmarket.crawler.model.ElementSelector;
import com.mesofi.mythclothmarket.crawler.model.LineUpDetection;
import com.mesofi.mythclothmarket.crawler.model.ListingStatus;
import com.mesofi.mythclothmarket.crawler.model.StoreName;
import com.mesofi.mythclothmarket.crawler.model.StorePageSelectors;

/**
 * Store crawler implementation for the Mandarake online store.
 * <p>
 * This crawler searches the English version of the Mandarake website for
 * Saint Seiya Myth Cloth products, extracts listing information from the
 * search results, and converts it into the application's internal listing
 * model.
 * </p>
 * <p>
 * Mandarake prices are always published in Japanese Yen (JPY). Product line
 * information is not explicitly identified by the crawler and is left for
 * later processing.
 * </p>
 */
@Component
public class MandarakeStoreCrawler extends AbstractPaginatedStoreCrawler {

    public MandarakeStoreCrawler(@Qualifier("playwrightHtmlFetcher") PageFetcher pageFetcher, CrawlerMapper mapper) {
        super(pageFetcher, mapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StoreName store() {
        return StoreName.MANDARAKE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String storeBaseUrl() {
        return "https://order.mandarake.co.jp";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getInitialSearchUrl() {
        return "/order/listPage/list?dispAdult=0&soldOut=1&keyword=myth%20cloth&lang=en";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getMaxPages() {
        return 10;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected StorePageSelectors selectors() {
        return new StorePageSelectors("div.block[data-adult=\"0\"]", "div.next a",
                new ElementSelector("div.title > p > a"), new ElementSelector("div.thum > a > img", "src"),
                new ElementSelector("div.thum > a", "href"), new ElementSelector("div.price > p"), null,
                new ElementSelector("div.addcart.addcart-text-en > a"));
    }

    @Override
    protected LineUpDetection determineLineUp(String nameText) {
        return new LineUpDetection(null, nameText);
    }

    @Override
    protected Currency determineCurrency(String priceText) {
        return Currency.getInstance("JPY");
    }

    @Override
    protected ListingStatus calculateListingStatus(String availabilityText) {
        return ListingStatus.IN_STOCK;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean includeStoreBaseUrl() {
        return true;
    }
}
