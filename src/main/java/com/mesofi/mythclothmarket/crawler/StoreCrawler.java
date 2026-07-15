package com.mesofi.mythclothmarket.crawler;

import java.util.List;

import com.mesofi.mythclothmarket.crawler.model.StoreListing;
import com.mesofi.mythclothmarket.crawler.model.StoreName;

public interface StoreCrawler {
    StoreName store();

    List<StoreListing> crawlListings();
}
