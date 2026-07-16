package com.mesofi.mythclothmarket.crawler.impl;

import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.mesofi.mythclothmarket.crawler.AbstractPaginatedStoreCrawler;
import com.mesofi.mythclothmarket.crawler.fetcher.PageFetcher;
import com.mesofi.mythclothmarket.crawler.mapper.CrawlerMapper;
import com.mesofi.mythclothmarket.crawler.mapper.RawStoreListing;
import com.mesofi.mythclothmarket.crawler.model.ListingStatus;
import com.mesofi.mythclothmarket.crawler.model.StoreName;
import com.mesofi.mythclothmarket.crawler.model.StorePageSelectors;

/**
 * Crawler implementation for BBST listing pages.
 */
@Component
public class BigBadToysStoreCrawler extends AbstractPaginatedStoreCrawler {

    /**
     * @param pageFetcher
     *            HTML fetcher used to retrieve store pages.
     * @param mapper
     *            mapper that converts scraped values to normalized listings.
     */
    protected BigBadToysStoreCrawler(@Qualifier("jsoupHtmlFetcher") PageFetcher pageFetcher, CrawlerMapper mapper) {
        super(pageFetcher, mapper);
    }

    /**
     * @return BigBadToyStore store identifier.
     */
    @Override
    public StoreName store() {
        return StoreName.BBTS;
    }

    /**
     * Parses a listing card. Not implemented yet.
     *
     * @param element
     *            listing card root element.
     * @return raw listing data when implemented.
     */
    @Override
    protected RawStoreListing parseListing(Element element) {
        return null;
    }

    /**
     * @return store base URL when implemented.
     */
    @Override
    protected String storeBaseUrl() {
        return "https://www.bigbadtoystore.com";
    }

    /**
     * @return initial search path when implemented.
     */
    @Override
    protected String getInitialSearchUrl() {
        return "/Search?SearchText=myth+cloth";
    }

    /**
     * @return page crawl limit when implemented.
     */
    @Override
    protected int getMaxPages() {
        return 10;
    }

    /**
     * @return selectors used by this crawler when implemented.
     */
    @Override
    protected StorePageSelectors selectors() {
        return null;
    }

    /**
     * Converts raw availability text into a normalized status. Not implemented yet.
     *
     * @param availabilityText
     *            raw availability label.
     * @return mapped listing status when implemented.
     */
    @Override
    protected ListingStatus calculateListingStatus(String availabilityText) {
        return null;
    }

}
