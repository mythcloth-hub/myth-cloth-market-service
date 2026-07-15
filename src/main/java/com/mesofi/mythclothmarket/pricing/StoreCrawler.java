package com.mesofi.mythclothmarket.pricing;

import java.util.List;

import com.mesofi.mythclothmarket.pricing.model.StoreListing;
import com.mesofi.mythclothmarket.pricing.model.StoreName;

public interface StoreCrawler {
    StoreName store();

    List<StoreListing> crawlListings();
}
