package com.mesofi.mythclothmarket.example;

import java.util.List;

import com.mesofi.mythclothmarket.crawler.AbstractPaginatedStoreCrawler;
import com.mesofi.mythclothmarket.crawler.NinNinGameStoreCrawler;
import com.mesofi.mythclothmarket.crawler.fetcher.JsoupHtmlFetcher;
import com.mesofi.mythclothmarket.crawler.model.StoreListing;
import com.mesofi.mythclothmarket.crawler.model.StoreName;

public class NinNinGameJsoupExample {
    static void main(String[] args) {

        AbstractPaginatedStoreCrawler paginatedStoreCrawler = new NinNinGameStoreCrawler(new JsoupHtmlFetcher());

        StoreName storeName = paginatedStoreCrawler.store();
        List<StoreListing> storeListings = paginatedStoreCrawler.crawlListings();

        System.out.println("Store: " + storeName);
        System.out.println("Listings: " + storeListings.size());
    }
}
