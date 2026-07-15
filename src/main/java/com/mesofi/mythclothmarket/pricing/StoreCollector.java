package com.mesofi.mythclothmarket.pricing;

import java.util.List;

import com.mesofi.mythclothmarket.pricing.model.StoreName;
import com.mesofi.mythclothmarket.pricing.model.StorePrice;

public interface StoreCollector {
    StoreName store();

    List<StorePrice> collectPrices();
}
