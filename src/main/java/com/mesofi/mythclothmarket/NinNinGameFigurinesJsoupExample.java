package com.mesofi.mythclothmarket;

import java.io.IOException;
import java.util.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class NinNinGameFigurinesJsoupExample {
    private static final String INITIAL_SEARCH_URL = "https://www.nin-nin-game.com/en/myth-cloth-saint-seiya";
    private static final String FIGURINE_ITEM_SELECTOR = ".general_block_card.ajax_block_product.item";
    private static final String NEXT_PAGE_SELECTOR = "#pagination_next_bottom a";

    static void main(String[] args) throws IOException {
        System.out.println("Starting JSOUP scraper for Nin-Nin-Game");

        List<MarketPriceStore> marketPriceStoreList = new LinkedList<>();
        String url = INITIAL_SEARCH_URL;
        int pageCount = 0;

        while (url != null && pageCount < 100) {
            pageCount++;

            Document doc = fetchDocument(url);
            if (doc == null) {
                break;
            }

            Elements figurineItems = doc.select(FIGURINE_ITEM_SELECTOR);
            System.out.println("Found " + figurineItems.size() + " figurine items on this page");

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

        System.out.println("Total figurines: " + marketPriceStoreList.size());
    }

    private static Document fetchDocument(String url) {
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

    private static String getNextPageUrl(Document doc) {
        Element nextPageLink = doc.selectFirst(NEXT_PAGE_SELECTOR);
        if (nextPageLink != null) {
            String href = nextPageLink.attr("href");
            if (!href.isEmpty()) {
                if (href.startsWith("http")) {
                    return href;
                } else if (href.startsWith("/")) {
                    return "https://www.nin-nin-game.com" + href;
                }
            }
        }
        return null;
    }
}
