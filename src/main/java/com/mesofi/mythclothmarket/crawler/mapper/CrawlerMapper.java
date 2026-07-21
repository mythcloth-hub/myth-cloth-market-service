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
 * Maps raw crawler fields into normalized {@link StoreListing} records.
 */
@Mapper(componentModel = "spring")
public interface CrawlerMapper {

    // Pre-compile the regex pattern once to improve parsing performance
    // significantly
    Pattern PRICE_PATTERN = Pattern.compile("[0-9., ]+");
    Pattern DISCOUNT_PATTERN = Pattern.compile("\\d+");

    /**
     * Converts raw scraped values into a normalized listing.
     *
     * @param raw
     *            raw listing values extracted from HTML.
     * @param storeName
     *            store identifier for the listing.
     * @param calculateListingStatus
     *            function that maps raw availability text to listing status.
     * @return normalized {@link StoreListing} instance.
     */
    @Mapping(target = "store", expression = "java(storeName)")
    @Mapping(target = "productName", source = "rawName")
    @Mapping(target = "lineUp", expression = "java(lineUp)")
    @Mapping(target = "price", source = "price", qualifiedByName = "parsePrice")
    @Mapping(target = "discount", source = "discount", qualifiedByName = "parseDiscount")
    @Mapping(target = "discountedPrice", source = "raw", qualifiedByName = "calculateDiscountedPrice")
    @Mapping(target = "currency", expression = "java(calculateCurrency.apply(raw.getPrice()))")
    @Mapping(target = "productUrl", source = "url")
    @Mapping(target = "productImageUrl", source = "imageUrl")
    @Mapping(target = "status", expression = "java(calculateListingStatus.apply(raw.getAvailability()))")
    @Mapping(target = "checkedAt", expression = "java(Instant.now())")
    StoreListing toStoreListing(RawStoreListing raw, @Context StoreName storeName, @Context LineUp lineUp,
            @Context Function<String, Currency> calculateCurrency,
            @Context Function<String, ListingStatus> calculateListingStatus);

    /**
     * Parses the numeric portion of a raw price string.
     *
     * @param priceString
     *            raw price text.
     * @return parsed price, or {@code null} when parsing is not possible.
     */
    @Named("parsePrice")
    default BigDecimal parsePrice(String priceString) {
        if (priceString == null || priceString.isBlank()) {
            return null;
        }

        Matcher matcher = PRICE_PATTERN.matcher(priceString);
        if (matcher.find()) {
            try {
                // Remove commas to prevent NumberFormatException
                String cleanNumber = matcher.group().replace(",", "").replace(" ", "");
                return new BigDecimal(cleanNumber);
            } catch (NumberFormatException e) {
                // Protects your crawler from crashing if it encounters badly formed data (e.g.,
                // "1.2.3")
                return null;
            }
        }

        return null;
    }

    /**
     * Parses discount percentage from raw discount text.
     *
     * @param discountString
     *            raw discount text.
     * @return parsed discount percent, or {@code null} if unavailable/invalid.
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
     * Calculates discounted price using parsed base price and discount percentage.
     *
     * @param raw
     *            raw listing values.
     * @return discounted price, original price if no discount exists, or
     *         {@code null} when price is unavailable.
     */
    @Named("calculateDiscountedPrice")
    default BigDecimal calculateDiscountedPrice(RawStoreListing raw) {
        if (raw == null) {
            return null;
        }

        BigDecimal originalPrice = parsePrice(raw.getPrice());
        if (originalPrice == null) {
            return null;
        }

        BigDecimal discountPercent = parseDiscount(raw.getDiscount());
        // If there is no discount, the discounted price is simply the original price
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
