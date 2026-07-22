package com.mesofi.mythclothmarket.crawler.model;

/**
 * Identifies the external stores supported by the market crawler.
 * <p>
 * Each enum constant represents a specific retailer for which a crawler
 * implementation can retrieve product information such as pricing,
 * availability, and product details.
 */
public enum StoreName {

    /**
     * Nin-Nin-Game online store.
     */
    NIN_NIN_GAME,

    /**
     * Mandarake online store.
     */
    MANDARAKE,

    /**
     * Luna Park online store.
     */
    LUNA_PARK,

    /**
     * My Kombini online store.
     */
    MY_KOMBINI
}
