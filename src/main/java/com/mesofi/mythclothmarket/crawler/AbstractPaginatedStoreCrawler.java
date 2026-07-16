package com.mesofi.mythclothmarket.crawler;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mesofi.mythclothmarket.crawler.fetcher.PageFetcher;
import com.mesofi.mythclothmarket.crawler.mapper.CrawlerMapper;
import com.mesofi.mythclothmarket.crawler.mapper.RawStoreListing;
import com.mesofi.mythclothmarket.crawler.model.StoreListing;
import com.mesofi.mythclothmarket.crawler.model.StorePageSelectors;

public abstract class AbstractPaginatedStoreCrawler implements StoreCrawler {

    Logger log = LoggerFactory.getLogger(AbstractPaginatedStoreCrawler.class);

    private final PageFetcher pageFetcher;
    private final CrawlerMapper crawlerMapper;

    protected AbstractPaginatedStoreCrawler(PageFetcher pageFetcher, CrawlerMapper mapper) {
        this.pageFetcher = pageFetcher;
        this.crawlerMapper = mapper;
    }

    @Override
    public List<StoreListing> crawlListings() {
        List<RawStoreListing> marketPriceStoreList = new ArrayList<>();
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

        return marketPriceStoreList.stream().map(raw -> crawlerMapper.toStoreListing(raw, store())).toList();
    }

    protected abstract RawStoreListing parseListing(Element element);

    protected abstract String storeBaseUrl();

    protected abstract String getInitialSearchUrl();

    protected abstract int getMaxPages();

    protected abstract StorePageSelectors selectors();

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
