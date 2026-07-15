package com.mesofi.mythclothmarket;

import java.util.List;

import com.mesofi.mythclothmarket.pricing.NinNinGameStoreListingPaginatorCollector;
import com.mesofi.mythclothmarket.pricing.StoreListingPaginatorCollector;
import com.mesofi.mythclothmarket.pricing.model.StoreListing;
import com.mesofi.mythclothmarket.pricing.model.StoreName;

public class Test {
    static void main(String[] args) {

        StoreListingPaginatorCollector storeListingPaginatorCollector = new NinNinGameStoreListingPaginatorCollector();

        StoreName storeName = storeListingPaginatorCollector.store();
        List<StoreListing> storeListings = storeListingPaginatorCollector.retrieveStoreListingInfo();

    }
}
