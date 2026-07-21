package com.mesofi.mythclothmarket.crawler.impl;

import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.mesofi.mythclothmarket.crawler.AbstractPaginatedStoreCrawler;
import com.mesofi.mythclothmarket.crawler.fetcher.PageFetcher;
import com.mesofi.mythclothmarket.crawler.mapper.CrawlerMapper;
import com.mesofi.mythclothmarket.crawler.model.ElementSelector;
import com.mesofi.mythclothmarket.crawler.model.LineUp;
import com.mesofi.mythclothmarket.crawler.model.LineUpDetection;
import com.mesofi.mythclothmarket.crawler.model.ListingStatus;
import com.mesofi.mythclothmarket.crawler.model.StoreName;
import com.mesofi.mythclothmarket.crawler.model.StorePageSelectors;

/**
 * {@link com.mesofi.mythclothmarket.crawler.StoreCrawler} implementation for
 * the Nin-Nin-Game online store.
 * <p>
 * This crawler navigates the paginated Myth Cloth product listings published by
 * Nin-Nin-Game, extracts the raw product information from each listing, and
 * delegates the normalization of the scraped values to the shared crawler
 * infrastructure.
 * <p>
 * In addition to extracting product details, this implementation translates
 * Nin-Nin-Game specific currency prefixes and availability labels into the
 * application's normalized domain model.
 */
@Component
public class NinNinGameStoreCrawler extends AbstractPaginatedStoreCrawler {

    private static final Pattern CURRENCY_PREFIX_PATTERN = Pattern.compile("^[A-Za-z]+");

    /**
     * Ordered collection of lineup matchers used to identify the lineup from the
     * prefix of a Nin-Nin-Game product name.
     * <p>
     * The order of the matchers is significant. More specific aliases should appear
     * before more general ones to ensure the correct lineup is detected.
     */
    private static final List<LineUpMatcher> LINE_UP_MATCHERS = List.of(
            new LineUpMatcher(LineUp.MYTH_CLOTH_EX, compileAliases("myth cloth ex", "saint cloth myth ex")),
            new LineUpMatcher(LineUp.MYTH_CLOTH,
                    compileAliases("myth cloth", "saint cloth myth", "saint seiya cloth myth")),
            new LineUpMatcher(LineUp.APPENDIX, compileAliases("appendix")),
            new LineUpMatcher(LineUp.SAINT_CLOTH_LEGEND, compileAliases("myth cloth legend")),
            new LineUpMatcher(LineUp.CROWN, compileAliases("crown cloth")));

    /**
     * Associates a {@link LineUp} with the compiled pattern used to recognize it in
     * a Nin-Nin-Game product name prefix.
     *
     * @param lineUp
     *            the lineup represented by this matcher
     * @param pattern
     *            the compiled pattern used to identify the lineup
     */
    private record LineUpMatcher(LineUp lineUp, Pattern pattern) {

        /**
         * Determines whether the specified text matches this lineup.
         *
         * @param text
         *            the product name prefix to test
         * @return {@code true} if the text matches this lineup; {@code false} otherwise
         */
        boolean matches(String text) {
            return pattern.matcher(text).find();
        }
    }

    /**
     * Compiles a case-insensitive pattern that matches any of the specified lineup
     * aliases as complete words.
     * <p>
     * Each alias is escaped using {@link Pattern#quote(String)} so that it is
     * treated as a literal value rather than a regular expression.
     *
     * @param aliases
     *            the aliases that identify a lineup
     * @return a compiled pattern matching any of the supplied aliases
     */
    private static Pattern compileAliases(String... aliases) {
        return Pattern.compile(
                "\\b(?:" + Arrays.stream(aliases).map(Pattern::quote).collect(Collectors.joining("|")) + ")\\b",
                Pattern.CASE_INSENSITIVE);
    }

    /**
     * Creates a crawler for the Nin-Nin-Game storefront.
     *
     * @param pageFetcher
     *            the component responsible for retrieving the HTML pages
     * @param mapper
     *            the mapper that converts raw scraped values into normalized
     *            {@code StoreListing} instances
     */
    public NinNinGameStoreCrawler(@Qualifier("jsoupHtmlFetcher") PageFetcher pageFetcher, CrawlerMapper mapper) {
        super(pageFetcher, mapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StoreName store() {
        return StoreName.NIN_NIN_GAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String storeBaseUrl() {
        return "https://www.nin-nin-game.com";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getInitialSearchUrl() {
        return "/en/myth-cloth-saint-seiya";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaxPages() {
        return 30;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StorePageSelectors selectors() {
        return new StorePageSelectors(".general_block_card.ajax_block_product.item", "#pagination_next_bottom a",
                new ElementSelector("a.product-name", "title"),
                new ElementSelector("div.product_image > a > img", "src"),
                new ElementSelector("div.product_image > a", "href"), new ElementSelector("span.price"),
                new ElementSelector("span.pill.orange"),
                new ElementSelector("div.actions > button, div.actions > span"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LineUpDetection determineLineUp(String nameText) {
        int separator = nameText.indexOf('-');
        if (separator < 0) {
            return new LineUpDetection(null, nameText);
        }

        String prefix = nameText.substring(0, separator).trim();
        String normalizedName = nameText.substring(separator + 1).trim();

        for (LineUpMatcher matcher : LINE_UP_MATCHERS) {
            if (matcher.matches(prefix)) {
                return new LineUpDetection(matcher.lineUp(), normalizedName);
            }
        }

        return new LineUpDetection(null, normalizedName);
    }

    /**
     * Determines the currency of a listing by inspecting the currency prefix
     * contained in the raw price text.
     * <p>
     * Nin-Nin-Game prefixes prices with abbreviations such as {@code MEX},
     * {@code USD}, {@code EUR}, or {@code JPY}. These prefixes are mapped to their
     * corresponding ISO 4217 currencies.
     *
     * @param priceText
     *            the raw price text extracted from the listing
     * @return the resolved currency, or {@code null} if the prefix is unknown or
     *         cannot be determined
     */
    @Override
    public Currency determineCurrency(String priceText) {
        if (priceText == null || priceText.isBlank()) {
            return null;
        }

        Matcher matcher = CURRENCY_PREFIX_PATTERN.matcher(priceText);
        if (matcher.find()) {
            String prefix = matcher.group().toUpperCase();

            try {
                return switch (prefix) {
                    case "MEX" -> Currency.getInstance("MXN"); // Mexican Peso
                    case "US", "USD" -> Currency.getInstance("USD"); // US Dollar
                    case "EUR" -> Currency.getInstance("EUR"); // Euro
                    case "JPY" -> Currency.getInstance("JPY"); // Japanese Yen
                    default -> null; // Unknown prefix, fallback to null
                };
            } catch (IllegalArgumentException e) {
                // Protects your mapper if an unsupported ISO 4217 code is passed
                return null;
            }
        }
        return null;
    }

    /**
     * Converts Nin-Nin-Game availability labels into normalized listing statuses.
     * <p>
     * The crawler maps the store-specific availability text into the corresponding
     * {@link ListingStatus} understood by the application.
     *
     * @param availabilityText
     *            the raw availability text extracted from the listing
     * @return the normalized listing status, or {@code null} if the availability
     *         cannot be determined
     */
    @Override
    public ListingStatus calculateListingStatus(String availabilityText) {
        if (availabilityText == null || availabilityText.isBlank()) {
            return null;
        }

        return switch (availabilityText.toLowerCase()) {
            case "add to cart" -> ListingStatus.IN_STOCK;
            case "soon available" -> ListingStatus.OUT_OF_STOCK;
            default -> ListingStatus.UNKNOWN;
        };
    }
}
