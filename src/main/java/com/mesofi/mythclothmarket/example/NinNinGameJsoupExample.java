package com.mesofi.mythclothmarket.example;

import java.util.List;

import com.mesofi.mythclothmarket.crawler.AbstractPaginatedStoreCrawler;
import com.mesofi.mythclothmarket.crawler.fetcher.JsoupHtmlFetcher;
import com.mesofi.mythclothmarket.crawler.impl.NinNinGameStoreCrawler;
import com.mesofi.mythclothmarket.crawler.mapper.CrawlerMapperImpl;
import com.mesofi.mythclothmarket.crawler.model.StoreListing;
import com.mesofi.mythclothmarket.crawler.model.StoreName;

/**
 * Standalone Jsoup crawler example for Nin-Nin-Game.
 */
public class NinNinGameJsoupExample {
    /**
     * Runs the Jsoup-based crawler example and prints normalized listing output.
     *
     * @param args
     *            unused command-line arguments.
     */
    static void main(String[] args) {

        AbstractPaginatedStoreCrawler paginatedStoreCrawler = new NinNinGameStoreCrawler(new JsoupHtmlFetcher(),
                new CrawlerMapperImpl());

        StoreName storeName = paginatedStoreCrawler.store();
        List<StoreListing> storeListings = paginatedStoreCrawler.crawlListings();

        System.out.println("Store: " + storeName);
        System.out.println("Listings: " + storeListings.size());
        for (StoreListing storeListing : storeListings) {
            System.out.println("Data: " + storeListing);
        }
    }
}
