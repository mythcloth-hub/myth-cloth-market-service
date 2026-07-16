package com.mesofi.mythclothmarket.crawler;

import java.util.Optional;

import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import com.mesofi.mythclothmarket.crawler.fetcher.PageFetcher;
import com.mesofi.mythclothmarket.crawler.mapper.CrawlerMapper;
import com.mesofi.mythclothmarket.crawler.mapper.RawStoreListing;
import com.mesofi.mythclothmarket.crawler.model.ListingStatus;
import com.mesofi.mythclothmarket.crawler.model.StoreName;
import com.mesofi.mythclothmarket.crawler.model.StorePageSelectors;

/**
 * Crawler implementation for Nin-Nin-Game listing pages.
 */
@Component
public class NinNinGameStoreCrawler extends AbstractPaginatedStoreCrawler {

    /**
     * @param pageFetcher
     *            HTML fetcher used to retrieve Nin-Nin-Game pages.
     * @param mapper
     *            mapper that converts scraped values to normalized listings.
     */
    public NinNinGameStoreCrawler(PageFetcher pageFetcher, CrawlerMapper mapper) {
        super(pageFetcher, mapper);
    }

    /**
     * @return Nin-Nin-Game store identifier.
     */
    @Override
    public StoreName store() {
        return StoreName.NIN_NIN_GAME;
    }

    /**
     * Extracts raw listing fields from a Nin-Nin-Game listing card element.
     *
     * @param element
     *            listing card root element.
     * @return raw listing values extracted from the card.
     */
    @Override
    public RawStoreListing parseListing(Element element) {
        RawStoreListing priceStore = new RawStoreListing();

        Optional.ofNullable(element.selectFirst(selectors().productName())).ifPresent(linkElement -> {
            priceStore.setFigurineRawName(linkElement.attr("title"));
            priceStore.setLink(linkElement.attr("href"));
        });

        Optional.ofNullable(element.selectFirst(selectors().priceContainer())).ifPresent(priceContainerElement -> {
            Optional.ofNullable(priceContainerElement.selectFirst(selectors().price()))
                    .ifPresent(price -> priceStore.setPrice(price.text()));
            Optional.ofNullable(priceContainerElement.selectFirst(selectors().discount()))
                    .ifPresent(discount -> priceStore.setDiscount(discount.text()));
        });

        Optional.ofNullable(element.selectFirst(selectors().availability()))
                .ifPresent(availabilityElement -> priceStore.setAvailability(availabilityElement.text()));

        return priceStore;
    }

    /**
     * @return Nin-Nin-Game base URL.
     */
    @Override
    public String storeBaseUrl() {
        return "https://www.nin-nin-game.com";
    }

    /**
     * @return category path used as the initial listing page.
     */
    @Override
    public String getInitialSearchUrl() {
        return "/en/myth-cloth-saint-seiya";
    }

    /**
     * @return crawl page limit for Nin-Nin-Game.
     */
    @Override
    public int getMaxPages() {
        return 30;
    }

    /**
     * @return selectors for listing cards, prices, availability, and pagination.
     */
    @Override
    public StorePageSelectors selectors() {
        return new StorePageSelectors(".general_block_card.ajax_block_product.item", "#pagination_next_bottom a",
                "a.product-name", "div.price_container", "span.price", "span.pill.orange",
                "div.actions > button, div.actions > span");
    }

    /**
     * Converts Nin-Nin-Game availability labels into normalized statuses.
     *
     * @param availabilityText
     *            raw availability label.
     * @return mapped listing status, or {@code null} when text is blank.
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
