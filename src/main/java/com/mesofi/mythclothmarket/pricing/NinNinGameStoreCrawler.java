package com.mesofi.mythclothmarket.pricing;

import java.util.Optional;

import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import com.mesofi.mythclothmarket.MarketPriceStore;
import com.mesofi.mythclothmarket.pricing.fetcher.PageFetcher;
import com.mesofi.mythclothmarket.pricing.model.StoreName;

@Component
public class NinNinGameStoreCrawler extends AbstractPaginatedStoreCrawler {

    public NinNinGameStoreCrawler(PageFetcher pageFetcher) {
        super(pageFetcher);
    }

    @Override
    public StoreName store() {
        return StoreName.NIN_NIN_GAME;
    }

    @Override
    public MarketPriceStore parseListing(Element element) {
        MarketPriceStore priceStore = new MarketPriceStore();

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
    public StoreSelectors selectors() {
        return new StoreSelectors(".general_block_card.ajax_block_product.item", "#pagination_next_bottom a",
                "a.product-name", "div.price_container", "span.price", "span.pill.orange");
    }
}
