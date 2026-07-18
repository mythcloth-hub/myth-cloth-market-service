package com.mesofi.mythclothmarket.crawler.impl;

import java.util.Currency;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.mesofi.mythclothmarket.crawler.AbstractPaginatedStoreCrawler;
import com.mesofi.mythclothmarket.crawler.fetcher.PageFetcher;
import com.mesofi.mythclothmarket.crawler.mapper.CrawlerMapper;
import com.mesofi.mythclothmarket.crawler.model.ElementSelector;
import com.mesofi.mythclothmarket.crawler.model.LineUp;
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
    public LineUp determineLineUp(String nameText) {
        if (nameText.contains("ex")) {
            return LineUp.MYTH_CLOTH_EX;
        }
        if (nameText.contains("appendix")) {
            return LineUp.APPENDIX;
        }
        return LineUp.MYTH_CLOTH;
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
