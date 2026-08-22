package com.mesofi.mythclothmarket.crawler.impl.jp;

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
 * the Jungle online store.
 * <p>
 * This crawler traverses Jungle's paginated Myth Cloth search results, extracts
 * raw listing data, and delegates normalization to the shared crawler
 * infrastructure.
 * <p>
 * Besides applying the storefront's fixed pricing and availability rules, this
 * implementation detects lineup aliases from the beginning of product titles.
 */
@Component
public class JungleStoreCrawler extends AbstractPaginatedStoreCrawler {

    /**
     * Ordered lineup aliases matched against the beginning of the product name.
     * <p>
     * Matchers are evaluated in declaration order, so more specific aliases must
     * appear before broader ones.
     */
    private static final List<LineUpMatcher> LINE_UP_MATCHERS = List.of(
            new LineUpMatcher(LineUpType.MYTH_CLOTH_EX, compileAliases("cloth myth ex")),
            new LineUpMatcher(LineUpType.MYTH_CLOTH, compileAliases("myth cloth", "cloth myth")));

    /**
     * Creates a crawler for the Jungle storefront.
     *
     * @param pageFetcher
     *            the component responsible for retrieving the HTML pages
     * @param mapper
     *            the mapper that converts raw scraped values into normalized
     *            {@code StoreListing} instances
     */
    protected JungleStoreCrawler(@Qualifier("jsoupHtmlFetcher") PageFetcher pageFetcher, CrawlerMapper mapper) {
        super(pageFetcher, mapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StoreName store() {
        return StoreName.JUNGLE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URI storeBaseUrl() {
        return StoreName.JUNGLE.website();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getInitialSearchUrl() {
        return "/products/list?category_id=&rank=&orderby=&name=myth+cloth";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaxPages() {
        return 20;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected StorePageSelectors selectors() {
        return new StorePageSelectors(".ec-shelfGrid__item", "li.ec-pager__item.item--next a",
                new ElementSelector("h3.ec-productItemRole__title span"),
                new ElementSelector("div.ec-productItemRole__image img", "src"),
                new ElementSelector("div.ec-productItemRole__image a", "href"),
                new ElementSelector("span.ec-price__price"), null, null, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<LineUpMatcher> getLineUpMatchers() {
        return LINE_UP_MATCHERS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Currency determineCurrency(String priceText) {
        return Currency.getInstance("JPY");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ListingStatus calculateListingStatus(String availabilityText) {
        return ListingStatus.IN_STOCK;
    }

}
