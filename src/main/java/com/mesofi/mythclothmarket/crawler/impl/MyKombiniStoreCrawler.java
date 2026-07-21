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
 * the MyKombini online store.
 * <p>
 * This crawler retrieves Myth Cloth product listings from MyKombini's paginated
 * search results and extracts product information such as the product name,
 * image URL, product URL, price, and availability.
 * <p>
 * The extracted values are converted into normalized {@code StoreListing}
 * instances by the shared crawler infrastructure.
 */
@Component
public class MyKombiniStoreCrawler extends AbstractPaginatedStoreCrawler {

    /**
     * Creates a crawler for the MyKombini storefront.
     *
     * @param pageFetcher
     *            the component responsible for retrieving HTML pages
     * @param mapper
     *            the mapper that converts raw scraped values into normalized store
     *            listings
     */
    public MyKombiniStoreCrawler(@Qualifier("jsoupHtmlFetcher") PageFetcher pageFetcher, CrawlerMapper mapper) {
        super(pageFetcher, mapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StoreName store() {
        return StoreName.MY_KOMBINI;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String storeBaseUrl() {
        return "https://mykombini.com";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getInitialSearchUrl() {
        return "/en/Research?orderby=position&orderway=desc&search_query=myth+cloth&submit_search=OK";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaxPages() {
        return 5;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StorePageSelectors selectors() {
        return new StorePageSelectors("li.ajax_block_product", "#pagination_next a",
                new ElementSelector("a.product_img_link", "title"),
                new ElementSelector("a.product_img_link > img", "src"),
                new ElementSelector("a.product_img_link", "href"), new ElementSelector("span.price"), null,
                new ElementSelector("a.exclusive.ajax_add_to_cart_button"));
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
     * Determines the currency used by MyKombini listings.
     * <p>
     * MyKombini publishes Myth Cloth prices in Japanese Yen, therefore all listings
     * are assigned the {@code JPY} currency.
     *
     * @param priceText
     *            the raw price text extracted from the listing
     * @return the Japanese Yen ({@code JPY}) currency
     */
    @Override
    public Currency determineCurrency(String priceText) {
        return Currency.getInstance("JPY");
    }

    /**
     * Converts MyKombini availability information into a normalized
     * {@link ListingStatus}.
     * <p>
     * Listings displaying the "Add to cart" action are considered to be in stock.
     * All other values are treated as out of stock.
     *
     * @param availabilityText
     *            the raw availability text extracted from the listing
     * @return the corresponding listing status
     */
    @Override
    public ListingStatus calculateListingStatus(String availabilityText) {
        if ("Add to cart".equals(availabilityText)) {
            return ListingStatus.IN_STOCK;
        }
        return ListingStatus.OUT_OF_STOCK;
    }
}
