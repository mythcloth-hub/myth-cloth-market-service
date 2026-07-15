package com.mesofi.mythclothmarket.crawler;

import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import com.mesofi.mythclothmarket.crawler.fetcher.PageFetcher;
import com.mesofi.mythclothmarket.crawler.model.MarketPriceStore;
import com.mesofi.mythclothmarket.crawler.model.StoreName;
import com.mesofi.mythclothmarket.crawler.model.StoreSelectors;

@Component
public class BigBadToysStoreCrawler extends AbstractPaginatedStoreCrawler {

    protected BigBadToysStoreCrawler(PageFetcher pageFetcher) {
        super(pageFetcher);
    }

    @Override
    public StoreName store() {
        return StoreName.BIG_BAD_TOY_STORE;
    }

    @Override
    protected MarketPriceStore parseListing(Element element) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String storeBaseUrl() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String getInitialSearchUrl() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected int getMaxPages() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    protected StoreSelectors selectors() {
        // TODO Auto-generated method stub
        return null;
    }

}
