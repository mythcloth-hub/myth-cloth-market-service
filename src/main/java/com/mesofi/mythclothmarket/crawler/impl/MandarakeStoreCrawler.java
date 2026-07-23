package com.mesofi.mythclothmarket.crawler.impl;

import static com.mesofi.mythclothmarket.utils.RegexUtils.compileAliases;

import java.util.Currency;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.mesofi.mythclothmarket.crawler.AbstractPaginatedStoreCrawler;
import com.mesofi.mythclothmarket.crawler.fetcher.PageFetcher;
import com.mesofi.mythclothmarket.crawler.mapper.CrawlerMapper;
import com.mesofi.mythclothmarket.crawler.model.ElementSelector;
import com.mesofi.mythclothmarket.crawler.model.LineUp;
import com.mesofi.mythclothmarket.crawler.model.LineUpDetection;
import com.mesofi.mythclothmarket.crawler.model.LineUpMatcher;
import com.mesofi.mythclothmarket.crawler.model.ListingStatus;
import com.mesofi.mythclothmarket.crawler.model.StoreName;
import com.mesofi.mythclothmarket.crawler.model.StorePageSelectors;

/**
 * {@link com.mesofi.mythclothmarket.crawler.StoreCrawler} implementation for
 * the Mandarake online store.
 * <p>
 * This crawler traverses the paginated Mandarake search results for Myth Cloth
 * products, extracts raw listing data, and delegates normalization to the
 * shared crawler infrastructure.
 * <p>
 * Mandarake listings are published in Japanese Yen and usually omit explicit
 * availability signals in the listing card. This implementation therefore uses
 * store-specific defaults for currency and status resolution.
 */
@Component
public class MandarakeStoreCrawler extends AbstractPaginatedStoreCrawler {

    private static final Pattern UNNECESSARY_WORDS_PATTERN = Pattern.compile(
            "\\b(?:bandainamco/bandaispirits|bandaispirits|bandai|spirits|amco/|namco|namco/|masami|kurumada|saint seiya)\\b",
            Pattern.CASE_INSENSITIVE);

    /**
     * Ordered lineup aliases matched against the beginning of the product name.
     * <p>
     * Matchers are evaluated in declaration order, so more specific aliases must
     * appear before broader ones.
     */
    private static final List<LineUpMatcher> LINE_UP_MATCHERS = List.of(
            new LineUpMatcher(LineUp.MYTH_CLOTH_EX, compileAliases("cloth myth ex", "myth cloth ex")),
            new LineUpMatcher(LineUp.APPENDIX, compileAliases("appendix", "appendix/appendix")),
            new LineUpMatcher(LineUp.MYTH_CLOTH, compileAliases("myth cloth", "cloth myth")));

    /**
     * Creates a crawler for the Mandarake storefront.
     *
     * @param pageFetcher
     *            the component responsible for retrieving the HTML pages
     * @param mapper
     *            the mapper that converts raw scraped values into normalized
     *            {@code StoreListing} instances
     */
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
        return StoreName.MANDARAKE.website().toString();
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

    /**
     * Detects lineup aliases from the beginning of the product title.
     * <p>
     * If no known alias is found, the original product title is preserved and the
     * lineup remains unresolved.
     *
     * @param nameText
     *            complete product title
     * @return detected lineup and normalized product name
     */
    @Override
    protected LineUpDetection determineLineUp(String nameText) {
        for (LineUpMatcher matcher : LINE_UP_MATCHERS) {
            if (matcher.matches(nameText)) {
                return new LineUpDetection(matcher.lineUp(), matcher.extractProductName(nameText));
            }
        }

        return new LineUpDetection(null, nameText);
    }

    /**
     * Resolves the currency used by Mandarake listings.
     * <p>
     * Mandarake search results for this crawler are always priced in Japanese Yen.
     *
     * @param priceText
     *            raw price text extracted from the listing
     * @return {@code JPY} for all listings
     */
    @Override
    protected Currency determineCurrency(String priceText) {
        return Currency.getInstance("JPY");
    }

    /**
     * Resolves availability for Mandarake listing cards.
     * <p>
     * The configured listing selector only includes entries with an active cart
     * action, so crawled items are treated as in stock.
     *
     * @param availabilityText
     *            raw availability text extracted from the listing
     * @return {@link ListingStatus#IN_STOCK} for all crawled listings
     */
    @Override
    protected ListingStatus calculateListingStatus(String availabilityText) {
        return ListingStatus.IN_STOCK;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean prependedStoreBaseUrlInImageUrl() {
        return true;
    }

    /**
     * Removes store and franchise branding noise from product titles.
     *
     * @param nameText
     *            raw product title
     * @return cleaned title used for subsequent lineup matching and mapping
     */
    @Override
    protected String removeUnnecessaryWords(String nameText) {
        return UNNECESSARY_WORDS_PATTERN.matcher(nameText).replaceAll("").replaceAll("\\s+", " ").trim();
    }

}
