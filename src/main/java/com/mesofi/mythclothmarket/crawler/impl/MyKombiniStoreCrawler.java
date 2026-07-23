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
 * the MyKombini online store.
 * <p>
 * This crawler traverses MyKombini's paginated Myth Cloth search results,
 * extracts raw listing data, and delegates normalization to the shared crawler
 * infrastructure.
 * <p>
 * Besides resolving MyKombini-specific availability labels, this implementation
 * contains custom lineup detection rules based on aliases at the beginning of
 * product titles.
 */
@Component
public class MyKombiniStoreCrawler extends AbstractPaginatedStoreCrawler {

    private static final Pattern UNNECESSARY_WORDS_PATTERN = Pattern.compile("\\b(?:bandai|spirits|saint seiya)\\b",
            Pattern.CASE_INSENSITIVE);

    /**
     * Ordered lineup aliases matched against the beginning of the product name.
     * <p>
     * Matchers are evaluated in declaration order, so more specific aliases must
     * appear before broader ones.
     */
    private static final List<LineUpMatcher> LINE_UP_MATCHERS = List.of(
            new LineUpMatcher(LineUp.MYTH_CLOTH_EX,
                    compileAliases("myth cloth ex", "saint cloth myth ex", "cloth myth ex", "myth ex")),
            new LineUpMatcher(LineUp.MYTH_CLOTH, compileAliases("myth cloth")));

    /**
     * Creates a crawler for the MyKombini storefront.
     *
     * @param pageFetcher
     *            the component responsible for retrieving the HTML pages
     * @param mapper
     *            the mapper that converts raw scraped values into normalized
     *            {@code StoreListing} instances
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
        return StoreName.MY_KOMBINI.website().toString();
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
     * Resolves the currency used by MyKombini listings.
     * <p>
     * MyKombini publishes Myth Cloth prices in Japanese Yen, therefore all listings
     * are assigned the {@code JPY} currency.
     *
     * @param priceText
     *            raw price text extracted from the listing
     * @return {@code JPY} for all listings
     */
    @Override
    public Currency determineCurrency(String priceText) {
        return Currency.getInstance("JPY");
    }

    /**
     * Resolves MyKombini availability labels into normalized listing statuses.
     * <p>
     * Listings displaying the "Add to cart" action are considered to be in stock.
     * All other values are treated as out of stock.
     *
     * @param availabilityText
     *            raw availability text extracted from the listing
     * @return normalized listing status for the given availability label
     */
    @Override
    public ListingStatus calculateListingStatus(String availabilityText) {
        if ("Add to cart".equals(availabilityText)) {
            return ListingStatus.IN_STOCK;
        }
        return ListingStatus.OUT_OF_STOCK;
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
