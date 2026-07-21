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
 * {@link com.mesofi.mythclothmarket.crawler.StoreCrawler} implementation for
 * the Luna Park online store.
 * <p>
 * This crawler retrieves Myth Cloth product listings from the Luna Park
 * storefront by navigating its paginated search results and extracting product
 * information such as the product name, URL, and price. The extracted data is
 * converted into domain objects by the shared crawler infrastructure for
 * downstream processing.
 * <p>
 * The crawler uses a {@link PageFetcher} backed by Jsoup to retrieve HTML
 * content and relies on store-specific CSS selectors to locate product
 * information within each page.
 */
@Component
public class LunaParkStoreCrawler extends AbstractPaginatedStoreCrawler {

    /**
     * Creates a crawler for the Luna Park storefront.
     *
     * @param pageFetcher
     *            the component responsible for retrieving HTML pages
     * @param mapper
     *            the mapper that converts raw scraped values into normalized store
     *            listings
     */
    public LunaParkStoreCrawler(@Qualifier("jsoupHtmlFetcher") PageFetcher pageFetcher, CrawlerMapper mapper) {
        super(pageFetcher, mapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StoreName store() {
        return StoreName.LUNA_PARK;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String storeBaseUrl() {
        return "https://www.lunapark.store";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getInitialSearchUrl() {
        return "/search?q=myth+cloth";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaxPages() {
        return 2;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StorePageSelectors selectors() {
        return new StorePageSelectors("li[data-hook=\"product-list-grid-item\"]",
                "[data-hook=\"product-list-pagination-seo\"] a[data-hook=\"product-list-pagination-link-seo-link\"]",
                new ElementSelector("p[data-hook=\"product-item-name\"]"),
                new ElementSelector(
                        "li[data-hook=\"product-list-grid-item\"] [data-hook=\"ProductMediaDataHook.Images\"] img:first-of-type",
                        "src"),
                new ElementSelector("a[data-hook=\"product-item-container\"]", "href"),
                new ElementSelector("span[data-hook=\"product-item-price-to-pay\"]"), null, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LineUpDetection determineLineUp(String nameText) {
        /*
         * if (nameText.contains("ex")) { return LineUp.MYTH_CLOTH_EX; } return
         * LineUp.MYTH_CLOTH;
         */
        return null;
    }

    /**
     * Determines the currency used by Luna Park product listings.
     *
     * @param priceText
     *            the raw price text extracted from the page
     * @return the currency associated with the listing price
     */
    @Override
    public Currency determineCurrency(String priceText) {
        return Currency.getInstance("JPY");
    }

    /**
     * Determines the listing status based on the extracted availability text.
     * <p>
     * Luna Park currently exposes only products that are available for purchase,
     * therefore every listing is considered {@link ListingStatus#IN_STOCK}.
     *
     * @param availabilityText
     *            the raw availability text extracted from the page
     * @return the calculated listing status
     */
    @Override
    public ListingStatus calculateListingStatus(String availabilityText) {
        return ListingStatus.IN_STOCK;
    }

}
