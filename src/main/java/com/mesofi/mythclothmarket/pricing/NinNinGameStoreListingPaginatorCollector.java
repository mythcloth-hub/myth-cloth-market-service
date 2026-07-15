package com.mesofi.mythclothmarket.pricing;

import com.mesofi.mythclothmarket.pricing.model.StoreName;

public class NinNinGameStoreListingPaginatorCollector implements StoreListingPaginatorCollector {

    @Override
    public String storeBaseUrl() {
        return "https://www.nin-nin-game.com";
    }
    @Override
    public StoreName store() {
        return StoreName.NIN_NIN_GAME;
    }

    @Override
    public int getMaxPages() {
        return 30;
    }

    @Override
    public String getInitialSearchUrl() {
        return "/en/myth-cloth-saint-seiya";
    }

    @Override
    public String getFigurineItemSelector() {
        return ".general_block_card.ajax_block_product.item";
    }

    @Override
    public String getNextPageSelector() {
        return "#pagination_next_bottom a";
    }
}
