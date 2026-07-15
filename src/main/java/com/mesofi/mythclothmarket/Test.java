package com.mesofi.mythclothmarket;

import java.util.List;

import com.mesofi.mythclothmarket.pricing.AbstractPaginatedStoreCrawler;
import com.mesofi.mythclothmarket.pricing.NinNinGameStoreCrawler;
import com.mesofi.mythclothmarket.pricing.fetcher.JsoupHtmlFetcher;
import com.mesofi.mythclothmarket.pricing.model.StoreListing;
import com.mesofi.mythclothmarket.pricing.model.StoreName;

public class Test {
    static void main(String[] args) {

        AbstractPaginatedStoreCrawler paginatedStoreCrawler = new NinNinGameStoreCrawler(new JsoupHtmlFetcher());

        StoreName storeName = paginatedStoreCrawler.store();
        List<StoreListing> storeListings = paginatedStoreCrawler.crawlListings();
    }
}
