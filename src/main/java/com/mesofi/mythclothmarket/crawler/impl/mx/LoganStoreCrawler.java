package com.mesofi.mythclothmarket.crawler.impl.mx;

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
import com.mesofi.mythclothmarket.crawler.model.LineUp;
import com.mesofi.mythclothmarket.crawler.model.LineUpMatcher;
import com.mesofi.mythclothmarket.crawler.model.ListingStatus;
import com.mesofi.mythclothmarket.crawler.model.StoreName;
import com.mesofi.mythclothmarket.crawler.model.StorePageSelectors;

/**
 * {@link com.mesofi.mythclothmarket.crawler.StoreCrawler} implementation for
 * the Logan Store online store.
 * <p>
 * This crawler traverses Logan Store's paginated Myth Cloth search results,
 * extracts raw listing data, and delegates normalization to the shared crawler
 * infrastructure.
 * <p>
 * Besides store-specific lineup alias detection, this implementation applies
 * fixed resolution rules for currency and availability based on the storefront
 * conventions used by Logan Store.
 */
@Component
public class LoganStoreCrawler extends AbstractPaginatedStoreCrawler {

    /**
     * Ordered lineup aliases matched against the beginning of the product name.
     * <p>
     * Matchers are evaluated in declaration order, so more specific aliases must
     * appear before broader ones.
     */
    private static final List<LineUpMatcher> LINE_UP_MATCHERS = List.of(
            new LineUpMatcher(LineUp.MYTH_CLOTH_EX,
                    compileAliases("saint seiya myth cloth ex", "saint cloth myth ex", "myth cloth ex", "myth ex")),
            new LineUpMatcher(LineUp.MYTH_CLOTH, compileAliases("saint seiya saint cloth myth", "saint cloth myth")));

    /**
     * Creates a crawler for the Logan Store storefront.
     *
     * @param pageFetcher
     *            the component responsible for retrieving the HTML pages
     * @param mapper
     *            the mapper that converts raw scraped values into normalized
     *            {@code StoreListing} instances
     */
    protected LoganStoreCrawler(@Qualifier("jsoupHtmlFetcher") PageFetcher pageFetcher, CrawlerMapper mapper) {
        super(pageFetcher, mapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StoreName store() {
        return StoreName.LOGAN_STORE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected URI storeBaseUrl() {
        return StoreName.LOGAN_STORE.website();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getInitialSearchUrl() {
        return "?s=myth+cloth&post_type=product&type_aws=true";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getMaxPages() {
        return 3;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected StorePageSelectors selectors() {
        return new StorePageSelectors("li.product.type-product", null, new ElementSelector("div.flex-fill > a"),
                new ElementSelector("div.tp-image-wrapper > img", "src"),
                new ElementSelector("div.flex-fill > a", "href"), new ElementSelector(".woocommerce-Price-amount bdi"),
                new ElementSelector("div.info-sale"), null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<LineUpMatcher> getLineUpMatchers() {
        return LINE_UP_MATCHERS;
    }

    /**
     * Resolves the currency used by Logan Store listings.
     * <p>
     * Logan Store publishes prices in Mexican Pesos, therefore all listings are
     * assigned the {@code MXN} currency.
     *
     * @param priceText
     *            raw price text extracted from the listing
     * @return {@code MXN} for all listings
     */
    @Override
    protected Currency determineCurrency(String priceText) {
        return Currency.getInstance("MXN");
    }

    /**
     * Resolves availability for Logan Store listing cards.
     * <p>
     * Logan Store currently exposes only products that are available for purchase,
     * therefore every listing is considered {@link ListingStatus#IN_STOCK}.
     *
     * @param availabilityText
     *            raw availability text extracted from the listing
     * @return {@link ListingStatus#IN_STOCK} for all crawled listings
     */
    @Override
    protected ListingStatus calculateListingStatus(String availabilityText) {
        return ListingStatus.IN_STOCK;
    }

}
