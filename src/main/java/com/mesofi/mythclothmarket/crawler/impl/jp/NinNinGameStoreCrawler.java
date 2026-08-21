package com.mesofi.mythclothmarket.crawler.impl.jp;

import static com.mesofi.mythclothmarket.utils.RegexUtils.compileAliases;

import java.io.IOException;
import java.net.URI;
import java.util.Currency;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.mesofi.mythclothmarket.crawler.AbstractPaginatedStoreCrawler;
import com.mesofi.mythclothmarket.crawler.fetcher.PageFetcher;
import com.mesofi.mythclothmarket.crawler.mapper.CrawlerMapper;
import com.mesofi.mythclothmarket.crawler.model.ElementSelector;
import com.mesofi.mythclothmarket.crawler.model.LineUpDetection;
import com.mesofi.mythclothmarket.crawler.model.LineUpMatcher;
import com.mesofi.mythclothmarket.crawler.model.LineUpType;
import com.mesofi.mythclothmarket.crawler.model.ListingStatus;
import com.mesofi.mythclothmarket.crawler.model.StoreName;
import com.mesofi.mythclothmarket.crawler.model.StorePageSelectors;

/**
 * {@link com.mesofi.mythclothmarket.crawler.StoreCrawler} implementation for
 * the Nin-Nin-Game online store.
 * <p>
 * This crawler traverses the paginated Myth Cloth catalog, extracts the raw
 * listing information, and delegates the normalization of scraped values to the
 * shared crawler infrastructure.
 * <p>
 * Besides translating Nin-Nin-Game specific currencies and availability labels,
 * this implementation customizes the lineup detection step inherited from
 * {@link AbstractPaginatedStoreCrawler}. Product titles are not consistently
 * formatted, so the crawler supports multiple separator styles before
 * delegating to alias-based matching through {@link #getLineUpMatchers()}.
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
            new LineUpMatcher(LineUpType.MYTH_CLOTH_EX, compileAliases("myth cloth ex", "saint cloth myth ex")),
            new LineUpMatcher(LineUpType.MYTH_CLOTH,
                    compileAliases("myth cloth", "saint cloth myth", "saint seiya cloth myth")),
            new LineUpMatcher(LineUpType.APPENDIX, compileAliases("appendix")),
            new LineUpMatcher(LineUpType.SAINT_CLOTH_LEGEND, compileAliases("myth cloth legend")),
            new LineUpMatcher(LineUpType.SAINT_CLOTH_CROWN, compileAliases("crown cloth")),
            new LineUpMatcher(LineUpType.DD_PANORAMATION, compileAliases("d.d.panoramation")));

    /**
     * Characters that separate the lineup portion from the character name in a
     * Nin-Nin-Game product title.
     * <p>
     * The separators are evaluated in declaration order. If none of them is
     * present, lineup detection falls back to the default matcher-based algorithm
     * provided by {@link AbstractPaginatedStoreCrawler#determineLineUp(String)}.
     */
    private static final List<Character> TOKEN_SEPARATORS = List.of('-', ':');

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
     * Prepares the Nin-Nin-Game request flow so the store renders prices in USD.
     * <p>
     * The fetcher performs a warmup request first, then posts the storefront
     * currency selector for the USD option and reuses the resulting session cookies
     * for the final page load. This keeps the selected currency aligned with the
     * HTML returned by the site.
     *
     * @return a function that posts the USD selection to Nin-Nin-Game and returns
     *         the server response
     */
    @Override
    public Function<Connection, Response> request() {
        return (conn) -> {
            try {
                return conn.method(Connection.Method.POST).data("id_currency", "2").data("SubmitCurrency", "1")
                        .execute();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
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
    public URI storeBaseUrl() {
        return StoreName.NIN_NIN_GAME.website();
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
                new ElementSelector("div.product_image > a", "href"),
                new ElementSelector(true, "div.price_container span.stroke, div.price_container span.price"),
                new ElementSelector("span.pill.orange"),
                new ElementSelector("div.actions > button, div.actions > span"), "div.box.preorder_date_box");
    }

    /**
     * {@inheritDoc}
     * <p>
     * Nin-Nin-Game does not use a single naming convention for its products.
     * Depending on the listing, the lineup may appear as:
     * <ul>
     * <li>a prefix separated by a dash, for example
     * {@code Myth Cloth EX - Gemini Saga};</li>
     * <li>a prefix separated by a colon, for example
     * {@code Myth Cloth EX: Gemini Saga}; or</li>
     * <li>the beginning of the product name without any explicit separator, for
     * example {@code Myth Cloth EX Gemini Saga}.</li>
     * </ul>
     * <p>
     * This implementation first attempts to split the title using the configured
     * separator characters. If no separator is found, it delegates to the default
     * matcher-based implementation from {@link AbstractPaginatedStoreCrawler},
     * which evaluates {@link #getLineUpMatchers()} against the beginning of the
     * product name and removes the matched lineup prefix from the normalized name.
     */
    @Override
    public LineUpDetection determineLineUp(String nameText) {
        for (char token : TOKEN_SEPARATORS) {
            int separator = nameText.indexOf(token);
            if (separator == -1) {
                continue;
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

        return super.determineLineUp(nameText);
    }

    /**
     * Provides the lineup matchers used by the parent class fallback algorithm.
     *
     * @return ordered lineup matchers for Nin-Nin-Game product titles
     */
    @Override
    protected List<LineUpMatcher> getLineUpMatchers() {
        return LINE_UP_MATCHERS;
    }

    /**
     * Determines the currency of a listing by inspecting the currency prefix
     * contained in the raw price text.
     * <p>
     * Nin-Nin-Game may render listing prices using different locale-specific
     * prefixes depending on the active currency session. This implementation
     * recognizes the known prefixes used by the storefront and maps them to the
     * corresponding ISO 4217 currencies, including {@code USD} for the forced USD
     * request flow used by this crawler.
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
            default -> ListingStatus.DISCONTINUED;
        };
    }

}
