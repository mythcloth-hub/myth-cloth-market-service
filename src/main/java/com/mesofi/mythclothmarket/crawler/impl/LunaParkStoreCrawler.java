package com.mesofi.mythclothmarket.crawler.impl;

import java.util.Currency;
import java.util.Optional;

import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.mesofi.mythclothmarket.crawler.AbstractPaginatedStoreCrawler;
import com.mesofi.mythclothmarket.crawler.fetcher.PageFetcher;
import com.mesofi.mythclothmarket.crawler.mapper.CrawlerMapper;
import com.mesofi.mythclothmarket.crawler.mapper.RawStoreListing;
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
     * Extracts the raw listing information from a Luna Park product element.
     * <p>
     * The returned {@link RawStoreListing} contains the scraped values exactly as
     * they appear on the page. Further normalization and mapping to the domain
     * model is performed by the shared crawler infrastructure.
     *
     * @param element
     *            the HTML element representing a single product listing
     * @return the extracted raw listing information
     */
    @Override
    protected RawStoreListing parseListing(Element element) {
        RawStoreListing priceStore = new RawStoreListing();

        Optional.ofNullable(element.selectFirst(selectors().productName()))
                .ifPresent(nameElement -> priceStore.setFigurineRawName(nameElement.text().trim()));

        // for now, I'm using priceContainer to get the link, it's OK for now.
        Optional.ofNullable(element.selectFirst(selectors().priceContainer()))
                .ifPresent(linkElement -> priceStore.setLink(linkElement.attr("href").trim()));

        Optional.ofNullable(element.selectFirst(selectors().price()))
                .ifPresent(priceElement -> priceStore.setPrice(priceElement.text().trim()));

        return priceStore;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String storeBaseUrl() {
        return "https://www.lunapark.store";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getInitialSearchUrl() {
        return "/search?q=myth+cloth";
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
        return new StorePageSelectors("li[data-hook=\"product-list-grid-item\"]",
                "[data-hook=\"product-list-pagination-seo\"] a[data-hook=\"product-list-pagination-link-seo-link\"]",
                "p[data-hook=\"product-item-name\"]", "a[data-hook=\"product-item-container\"]",
                "span[data-hook=\"product-item-price-to-pay\"]", null, null);
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
    protected ListingStatus calculateListingStatus(String availabilityText) {
        return ListingStatus.IN_STOCK;
    }

}
