package com.mesofi.mythclothmarket.pricing;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mesofi.mythclothmarket.MarketPriceStore;
import com.mesofi.mythclothmarket.pricing.model.StoreListing;
import com.mesofi.mythclothmarket.pricing.model.StoreName;

public interface StoreListingPaginatorCollector {

    Logger log = LoggerFactory.getLogger(StoreListingPaginatorCollector.class);

    default List<StoreListing> retrieveStoreListingInfo() {
        List<MarketPriceStore> marketPriceStoreList = new LinkedList<>();
        String url = storeBaseUrl() + getInitialSearchUrl();

        int pageCount = 0;
        while (url != null && pageCount < getMaxPages()) {
            pageCount++;

            Document doc = fetchDocument(url);
            if (doc == null) {
                break;
            }

            Elements figurineItems = doc.select(getFigurineItemSelector());
            log.info("Found {} figurine items on page {}", figurineItems.size(), pageCount);

            for (Element currFigurineItem : figurineItems) {
                MarketPriceStore priceStore = new MarketPriceStore();

                Optional.ofNullable(currFigurineItem.selectFirst("a.product-name")).ifPresent(linkElement -> {
                    priceStore.setFigurineRawName(linkElement.attr("title"));
                    priceStore.setLink(linkElement.attr("href"));
                });
                Optional.ofNullable(currFigurineItem.selectFirst("div.price_container"))
                        .ifPresent(priceContainerElement -> {
                            Optional.ofNullable(priceContainerElement.selectFirst("span.price"))
                                    .ifPresent(price -> priceStore.setPrice(price.text()));
                            Optional.ofNullable(priceContainerElement.selectFirst("span.pill.orange"))
                                    .ifPresent(discount -> priceStore.setDiscount(discount.text()));
                        });

                marketPriceStoreList.add(priceStore);
            }

            url = getNextPageUrl(doc);
        }

        System.out.println("Found " + marketPriceStoreList.size() + " market price stores");
        return List.of();
    }

    String storeBaseUrl();
    StoreName store();
    int getMaxPages();
    String getInitialSearchUrl();
    String getFigurineItemSelector();
    String getNextPageSelector();

    private Document fetchDocument(String url) {
        try {
            return Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 "
                            + "(KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36")
                    .header("Accept-Language", "en-US,en;q=0.9").header("Upgrade-Insecure-Requests", "1").timeout(30000)
                    .get();
        } catch (IOException e) {
            System.err.println("Error fetching URL: " + url);
            e.printStackTrace();
            return null;
        }
    }

    private String getNextPageUrl(Document doc) {
        Element nextPageLink = doc.selectFirst(getNextPageSelector());
        if (nextPageLink != null) {
            String href = nextPageLink.attr("href");
            if (!href.isEmpty()) {
                if (href.startsWith("http")) {
                    return href;
                } else if (href.startsWith("/")) {
                    return storeBaseUrl() + href;
                }
            }
        }
        return null;
    }
}
