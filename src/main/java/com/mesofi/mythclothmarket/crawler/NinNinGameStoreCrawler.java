package com.mesofi.mythclothmarket.crawler;

import java.util.Optional;

import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import com.mesofi.mythclothmarket.crawler.fetcher.PageFetcher;
import com.mesofi.mythclothmarket.crawler.mapper.CrawlerMapper;
import com.mesofi.mythclothmarket.crawler.mapper.RawStoreListing;
import com.mesofi.mythclothmarket.crawler.model.StoreName;
import com.mesofi.mythclothmarket.crawler.model.StorePageSelectors;

@Component
public class NinNinGameStoreCrawler extends AbstractPaginatedStoreCrawler {

    public NinNinGameStoreCrawler(PageFetcher pageFetcher, CrawlerMapper mapper) {
        super(pageFetcher, mapper);
    }

    @Override
    public StoreName store() {
        return StoreName.NIN_NIN_GAME;
    }

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

    @Override
    public String storeBaseUrl() {
        return "https://www.nin-nin-game.com";
    }

    @Override
    public String getInitialSearchUrl() {
        return "/en/myth-cloth-saint-seiya";
    }

    @Override
    public int getMaxPages() {
        return 30;
    }

    @Override
    public StorePageSelectors selectors() {
        return new StorePageSelectors(".general_block_card.ajax_block_product.item", "#pagination_next_bottom a",
                "a.product-name", "div.price_container", "span.price", "span.pill.orange",
                "div.actions > button, div.actions > span");
    }
}
