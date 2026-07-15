package com.mesofi.mythclothmarket.pricing;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mesofi.mythclothmarket.MarketPriceStore;
import com.mesofi.mythclothmarket.pricing.fetcher.PageFetcher;
import com.mesofi.mythclothmarket.pricing.model.StoreListing;

public abstract class AbstractPaginatedStoreCrawler implements StoreCrawler {

    Logger log = LoggerFactory.getLogger(AbstractPaginatedStoreCrawler.class);

    private final PageFetcher pageFetcher;

    protected AbstractPaginatedStoreCrawler(PageFetcher pageFetcher) {
        this.pageFetcher = pageFetcher;
    }

    @Override
    public List<StoreListing> crawlListings() {
        List<MarketPriceStore> marketPriceStoreList = new ArrayList<>();
        String url = storeBaseUrl() + getInitialSearchUrl();

        int pageCount = 0;
        while (url != null && pageCount < getMaxPages()) {
            pageCount++;

            String html = pageFetcher.fetch(url);
            if (html == null) {
                break;
            }

            Document doc = Jsoup.parse(html);
            Elements figurineItems = doc.select(selectors().item());
            log.info("Found {} figurine items on page {}", figurineItems.size(), pageCount);

            figurineItems.forEach(figurine -> marketPriceStoreList.add(parseListing(figurine)));

            url = getNextPageUrl(doc);
        }

        log.info("Finished retrieving store listing info for {}. Total pages: {}, Total items: {}", store(), pageCount,
                marketPriceStoreList.size());

        return List.of();
    }

    protected abstract MarketPriceStore parseListing(Element element);
    protected abstract String storeBaseUrl();
    protected abstract String getInitialSearchUrl();
    protected abstract int getMaxPages();
    protected abstract StoreSelectors selectors();

    private String getNextPageUrl(Document doc) {
        Element nextPageLink = doc.selectFirst(selectors().nextPage());
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
