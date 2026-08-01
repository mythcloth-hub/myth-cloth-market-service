package com.mesofi.mythclothmarket.crawler.impl.be;

import static com.mesofi.mythclothmarket.utils.RegexUtils.compileAliases;

import java.net.URI;
import java.util.Currency;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.mesofi.mythclothmarket.crawler.AbstractPaginatedStoreCrawler;
import com.mesofi.mythclothmarket.crawler.fetcher.PageFetcher;
import com.mesofi.mythclothmarket.crawler.mapper.CrawlerMapper;
import com.mesofi.mythclothmarket.crawler.model.ElementSelector;
import com.mesofi.mythclothmarket.crawler.model.LineUpMatcher;
import com.mesofi.mythclothmarket.crawler.model.LineUpType;
import com.mesofi.mythclothmarket.crawler.model.ListingStatus;
import com.mesofi.mythclothmarket.crawler.model.StoreName;
import com.mesofi.mythclothmarket.crawler.model.StorePageSelectors;

/**
 * {@link com.mesofi.mythclothmarket.crawler.StoreCrawler} implementation for
 * the Myth Factory online store.
 * <p>
 * This crawler traverses Myth Factory's paginated Saint Seiya search results,
 * extracts raw listing data, and delegates normalization to the shared crawler
 * infrastructure.
 * <p>
 * Besides store-specific lineup alias detection, this implementation applies
 * fixed resolution rules for currency and availability labels based on the
 * storefront conventions used by Myth Factory.
 */
@Component
public class MythFactoryStoreCrawler extends AbstractPaginatedStoreCrawler {

    /**
     * Ordered lineup aliases matched against the beginning of the product name.
     * <p>
     * Matchers are evaluated in declaration order, so more specific aliases must
     * appear before broader ones.
     */
    private static final List<LineUpMatcher> LINE_UP_MATCHERS = List.of(
            new LineUpMatcher(LineUpType.MYTH_CLOTH_EX, compileAliases("saint cloth myth ex", "myth cloth ex figure")),
            new LineUpMatcher(LineUpType.MYTH_CLOTH, compileAliases("saint cloth myth", "myth cloth figure")),
            new LineUpMatcher(LineUpType.FIGUARTS_ZERO, compileAliases("figuarts zero touche metallique")));

    /**
     * Creates a crawler for the Myth Factory storefront.
     *
     * @param pageFetcher
     *            the component responsible for retrieving the HTML pages
     * @param mapper
     *            the mapper that converts raw scraped values into normalized
     *            {@code StoreListing} instances
     */
    protected MythFactoryStoreCrawler(@Qualifier("playwrightHtmlFetcher") PageFetcher pageFetcher,
            CrawlerMapper mapper) {
        super(pageFetcher, mapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StoreName store() {
        return StoreName.MYTH_FACTORY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected URI storeBaseUrl() {
        return StoreName.MYTH_FACTORY.website();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getInitialSearchUrl() {
        return "/en/search?controller=search&s=saint+seiya";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getMaxPages() {
        return 4;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected StorePageSelectors selectors() {
        return new StorePageSelectors("article.product-miniature", "a.next.js-search-link",
                new ElementSelector("div.h3.product-title > a"),
                new ElementSelector("div.thumbnail-container img", "src"),
                new ElementSelector("div.thumbnail-container a", "href"),
                new ElementSelector(true,
                        "div.product-price-and-shipping > span.regular-price, div.product-price-and-shipping > span.price"),
                new ElementSelector("div.product-price-and-shipping > span.discount-amount.discount-product"),
                new ElementSelector("ul.product-flags > li.flag-oos"), "img.plabel_img");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<LineUpMatcher> getLineUpMatchers() {
        return LINE_UP_MATCHERS;
    }

    /**
     * Resolves the currency used by Myth Factory listings.
     * <p>
     * Myth Factory publishes prices in Euros, therefore all listings are assigned
     * the {@code EUR} currency.
     *
     * @param priceText
     *            raw price text extracted from the listing
     * @return {@code EUR} for all listings
     */
    @Override
    protected Currency determineCurrency(String priceText) {
        return Currency.getInstance("EUR");
    }

    /**
     * Resolves Myth Factory availability labels into normalized listing statuses.
     * <p>
     * Listings marked as "Out of stock" are considered out of stock. Any other
     * value is treated as in stock.
     *
     * @param availabilityText
     *            raw availability text extracted from the listing
     * @return normalized listing status for the given availability label
     */
    @Override
    protected ListingStatus calculateListingStatus(String availabilityText) {
        if ("Out of stock".equalsIgnoreCase(availabilityText)) {
            return ListingStatus.OUT_OF_STOCK;
        }
        return ListingStatus.IN_STOCK;
    }
}
