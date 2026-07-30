package com.mesofi.mythclothmarket.crawler.mapper;

import org.jsoup.nodes.Element;

import lombok.Getter;
import lombok.Setter;

/**
 * Raw listing fields extracted directly from a store's HTML before
 * normalization and conversion into the domain model.
 * <p>
 * Some fields, such as discount and availability, may be {@code null} if the
 * store does not provide that information.
 * </p>
 */
@Getter
@Setter
public class RawStoreListing {
    private String originalName;
    private String normalizedName;
    private String imageUrl;
    private String productUrl;
    private String priceText;
    private String discountText;
    private String availabilityText;
    private Element preorderElement;
}
