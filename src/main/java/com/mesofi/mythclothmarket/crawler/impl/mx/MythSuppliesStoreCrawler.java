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
import com.mesofi.mythclothmarket.crawler.model.LineUpMatcher;
import com.mesofi.mythclothmarket.crawler.model.LineUpType;
import com.mesofi.mythclothmarket.crawler.model.ListingStatus;
import com.mesofi.mythclothmarket.crawler.model.StoreName;
import com.mesofi.mythclothmarket.crawler.model.StorePageSelectors;

/**
 * {@link com.mesofi.mythclothmarket.crawler.StoreCrawler} implementation for
 * the Myth Supplies online store.
 * <p>
 * This crawler traverses Myth Supplies paginated Myth Cloth search results,
 * extracts raw listing data, and delegates normalization to the shared crawler
 * infrastructure.
 * <p>
 * Besides store-specific lineup alias detection, this implementation applies
 * fixed resolution rules for currency and availability labels based on the
 * storefront conventions used by Myth Supplies.
 */
@Component
public class MythSuppliesStoreCrawler extends AbstractPaginatedStoreCrawler {

    /**
     * Ordered lineup aliases matched against the beginning of the product name.
     * <p>
     * Matchers are evaluated in declaration order, so more specific aliases must
     * appear before broader ones.
     */
    private static final List<LineUpMatcher> LINE_UP_MATCHERS = List.of(
            new LineUpMatcher(LineUpType.MYTH_CLOTH_EX,
                    compileAliases("myth cloth metal ex", "saint cloth metal ex", "ex saint cloth myth",
                            "myth cloth ex", "metal ex", "ex")),
            new LineUpMatcher(LineUpType.MYTH_CLOTH, compileAliases("saint cloth myth", "myth cloth")));

    /**
     * Creates a crawler for the Myth Supplies storefront.
     *
     * @param pageFetcher
     *            the component responsible for retrieving the HTML pages
     * @param mapper
     *            the mapper that converts raw scraped values into normalized
     *            {@code StoreListing} instances
     */
    protected MythSuppliesStoreCrawler(@Qualifier("playwrightHtmlFetcher") PageFetcher pageFetcher,
            CrawlerMapper mapper) {
        super(pageFetcher, mapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StoreName store() {
        return StoreName.MYTH_SUPPLIES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected URI storeBaseUrl() {
        return StoreName.MYTH_SUPPLIES.website();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getInitialSearchUrl() {
        return "/buscar?s=myth+cloth&categoria=";
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
        return new StorePageSelectors("div.col-md-3", "a[rel=next]", new ElementSelector("div.info > h3"),
                new ElementSelector("div.block.producto img", "src"),
                new ElementSelector("div.block.producto > a", "href"), new ElementSelector("div.info > div.precio"),
                null, new ElementSelector("div.info button.btn.btn-success"), null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<LineUpMatcher> getLineUpMatchers() {
        return LINE_UP_MATCHERS;
    }

    /**
     * Resolves the currency used by Myth Supplies listings.
     * <p>
     * Myth Supplies publishes prices in Mexican Pesos, therefore all listings are
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
     * Resolves Myth Supplies availability labels into normalized listing statuses.
     * <p>
     * Listings showing the "Agregar" action are considered in stock. Other labels
     * are treated as out of stock.
     *
     * @param availabilityText
     *            raw availability text extracted from the listing
     * @return normalized listing status for the given availability label
     */
    @Override
    protected ListingStatus calculateListingStatus(String availabilityText) {
        return "Agregar".equalsIgnoreCase(availabilityText) ? ListingStatus.IN_STOCK : ListingStatus.OUT_OF_STOCK;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean prependedStoreBaseUrlInProductUrl() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean prependedStoreBaseUrlInImageUrl() {
        return true;
    }
}
