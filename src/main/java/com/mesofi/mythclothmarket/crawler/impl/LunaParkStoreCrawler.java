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
 * the Luna Park online store.
 * <p>
 * This crawler traverses Luna Park's paginated Myth Cloth search results,
 * extracts raw listing data, and delegates normalization to the shared crawler
 * infrastructure.
 * <p>
 * Besides applying store-specific title cleanup and lineup alias detection,
 * this implementation uses fixed resolution rules for currency and stock status
 * based on how Luna Park exposes listing data.
 */
@Component
public class LunaParkStoreCrawler extends AbstractPaginatedStoreCrawler {

    private static final Pattern UNNECESSARY_WORDS_PATTERN = Pattern.compile("\\b(?:japan version|bandai|saint)\\b",
            Pattern.CASE_INSENSITIVE);

    /**
     * Ordered lineup aliases matched against the beginning of the product name.
     * <p>
     * Matchers are evaluated in declaration order, so more specific aliases must
     * appear before broader ones.
     */
    private static final List<LineUpMatcher> LINE_UP_MATCHERS = List.of(
            new LineUpMatcher(LineUp.MYTH_CLOTH_EX, compileAliases("myth cloth ex", "cloth myth ex")),
            new LineUpMatcher(LineUp.MYTH_CLOTH, compileAliases("myth cloth", "cloth myth")));

    /**
     * Creates a crawler for the Luna Park storefront.
     *
     * @param pageFetcher
     *            the component responsible for retrieving the HTML pages
     * @param mapper
     *            the mapper that converts raw scraped values into normalized
     *            {@code StoreListing} instances
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
        return StoreName.LUNA_PARK.website().toString();
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
        return 4;
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
     * Resolves the currency used by Luna Park listings.
     * <p>
     * Luna Park publishes Myth Cloth prices in Japanese Yen, therefore all listings
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
     * Resolves availability for Luna Park listing cards.
     * <p>
     * Luna Park currently exposes only products that are available for purchase,
     * therefore every listing is considered {@link ListingStatus#IN_STOCK}.
     *
     * @param availabilityText
     *            raw availability text extracted from the listing
     * @return {@link ListingStatus#IN_STOCK} for all crawled listings
     */
    @Override
    public ListingStatus calculateListingStatus(String availabilityText) {
        return ListingStatus.IN_STOCK;
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

    /**
     * {@inheritDoc}
     */
    @Override
    protected String filterImageUrl(String imageUrl) {
        int index = imageUrl.toLowerCase().indexOf(".jpg");
        if (index >= 0) {
            return imageUrl.substring(0, index + 4);
        }
        return imageUrl;
    }
}
