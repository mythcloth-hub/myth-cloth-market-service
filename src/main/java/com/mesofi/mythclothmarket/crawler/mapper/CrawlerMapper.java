package com.mesofi.mythclothmarket.crawler.mapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.mesofi.mythclothmarket.crawler.model.LineUp;
import com.mesofi.mythclothmarket.crawler.model.ListingStatus;
import com.mesofi.mythclothmarket.crawler.model.StoreListing;
import com.mesofi.mythclothmarket.crawler.model.StoreName;

/**
 * Maps raw values extracted by store crawlers into normalized
 * {@link StoreListing} instances.
 * <p>
 * This mapper is responsible for converting scraped text into strongly typed
 * values, including prices, discounts, currencies, availability status, and
 * computed discounted prices.
 * </p>
 */
@Mapper(componentModel = "spring")
public interface CrawlerMapper {

    // Pre-compile the regex pattern once to improve parsing performance
    // significantly
    Pattern PRICE_PATTERN = Pattern.compile("[0-9., ]+");
    Pattern DISCOUNT_PATTERN = Pattern.compile("\\d+");

    /**
     * Converts a raw store listing into a normalized {@link StoreListing}.
     *
     * @param raw
     *            the raw listing values extracted from the store page.
     * @param storeName
     *            the store that produced the listing.
     * @param lineUp
     *            the detected figurine line-up, or {@code null} if it could not be
     *            determined.
     * @param calculateCurrency
     *            function that determines the listing currency from the raw price
     *            text.
     * @param calculateListingStatus
     *            function that converts the raw availability text into a
     *            {@link ListingStatus}.
     * @return the normalized store listing.
     */
    @Mapping(target = "store", expression = "java(storeName)")
    @Mapping(target = "lineUp", expression = "java(lineUp)")
    @Mapping(target = "originalProductName", source = "originalName")
    @Mapping(target = "productName", source = "normalizedName")
    @Mapping(target = "productImageUrl", source = "imageUrl")
    @Mapping(target = "productUrl", source = "productUrl")
    @Mapping(target = "price", source = "priceText", qualifiedByName = "parsePrice")
    @Mapping(target = "discount", source = "discountText", qualifiedByName = "parseDiscount")
    @Mapping(target = "discountedPrice", source = "raw", qualifiedByName = "calculateDiscountedPrice")
    @Mapping(target = "currency", expression = "java(calculateCurrency.apply(raw.getPriceText()))")
    @Mapping(target = "status", expression = "java(calculateListingStatus.apply(raw.getAvailabilityText()))")
    @Mapping(target = "checkedAt", expression = "java(Instant.now())")
    StoreListing toStoreListing(RawStoreListing raw, @Context StoreName storeName, @Context LineUp lineUp,
            @Context Function<String, Currency> calculateCurrency,
            @Context Function<String, ListingStatus> calculateListingStatus);

    /**
     * Extracts and parses the numeric value from a raw price string.
     *
     * @param priceString
     *            the raw price text.
     * @return the parsed price, or {@code null} if the price is missing or cannot
     *         be parsed.
     */
    @Named("parsePrice")
    default BigDecimal parsePrice(String priceString) {
        if (priceString == null || priceString.isBlank()) {
            return null;
        }

        Matcher matcher = PRICE_PATTERN.matcher(priceString);
        if (matcher.find()) {
            try {
                // Remove thousands separators and whitespace before parsing.
                String cleanNumber = matcher.group().replace(",", "").replace(" ", "");
                return new BigDecimal(cleanNumber);
            } catch (NumberFormatException e) {
                // Ignore malformed numeric values.
                return null;
            }
        }

        return null;
    }

    /**
     * Extracts and parses the discount percentage from raw discount text.
     *
     * @param discountString
     *            the raw discount text.
     * @return the parsed discount percentage, or {@code null} if no valid
     *         percentage could be extracted.
     */
    @Named("parseDiscount")
    default BigDecimal parseDiscount(String discountString) {
        if (discountString == null || discountString.isBlank()) {
            return null;
        }
        Matcher matcher = DISCOUNT_PATTERN.matcher(discountString);
        if (matcher.find()) {
            try {
                return new BigDecimal(matcher.group());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Calculates the discounted price for a listing.
     * <p>
     * If no discount is available, the original price is returned. If the original
     * price cannot be determined, {@code null} is returned.
     * </p>
     *
     * @param raw
     *            the raw listing values.
     * @return the discounted price, the original price when no discount applies, or
     *         {@code null} if the original price is unavailable.
     */
    @Named("calculateDiscountedPrice")
    default BigDecimal calculateDiscountedPrice(RawStoreListing raw) {
        if (raw == null) {
            return null;
        }

        BigDecimal originalPrice = parsePrice(raw.getPriceText());
        if (originalPrice == null) {
            return null;
        }

        BigDecimal discountPercent = parseDiscount(raw.getDiscountText());
        // No price available.
        if (discountPercent == null || discountPercent.compareTo(BigDecimal.ZERO) == 0) {
            return originalPrice;
        }

        // Formula: DiscountAmount = Price * (DiscountPercent / 100)
        BigDecimal discountFactor = discountPercent.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal discountAmount = originalPrice.multiply(discountFactor);

        // Formula: DiscountedPrice = Price - DiscountAmount
        // Set scale to 2 decimal places for financial calculations
        return originalPrice.subtract(discountAmount.abs()).setScale(2, RoundingMode.HALF_UP);
    }

}
