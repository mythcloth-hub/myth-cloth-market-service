package com.mesofi.mythclothmarket.pricing;

import com.mesofi.mythclothmarket.pricing.model.StoreName;

public class BigBadToyStoreListingPaginatorCollector implements StoreListingPaginatorCollector {

    @Override
    public String storeBaseUrl() {
        return "";
    }

    @Override
    public StoreName store() {
        return StoreName.BIG_BAD_TOY_STORE;
    }

    @Override
    public int getMaxPages() {
        return 0;
    }

    @Override
    public String getInitialSearchUrl() {
        return "";
    }

    @Override
    public String getFigurineItemSelector() {
        return "";
    }

    @Override
    public String getNextPageSelector() {
        return "";
    }
}
