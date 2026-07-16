package com.mesofi.mythclothmarket.crawler;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.function.Function;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mesofi.mythclothmarket.crawler.fetcher.PageFetcher;
import com.mesofi.mythclothmarket.crawler.mapper.CrawlerMapper;
import com.mesofi.mythclothmarket.crawler.mapper.RawStoreListing;
import com.mesofi.mythclothmarket.crawler.model.ListingStatus;
import com.mesofi.mythclothmarket.crawler.model.StoreListing;
import com.mesofi.mythclothmarket.crawler.model.StoreName;
import com.mesofi.mythclothmarket.crawler.model.StorePageSelectors;

/**
 * Base crawler for stores that expose listings across paginated HTML pages.
 */
public abstract class AbstractPaginatedStoreCrawler implements StoreCrawler {

    Logger log = LoggerFactory.getLogger(AbstractPaginatedStoreCrawler.class);

    private final PageFetcher pageFetcher;
    private final CrawlerMapper crawlerMapper;

    /**
     * @param pageFetcher
     *            strategy used to retrieve page HTML.
     * @param mapper
     *            mapper that converts raw scraped values to normalized listings.
     */
    protected AbstractPaginatedStoreCrawler(PageFetcher pageFetcher, CrawlerMapper mapper) {
        this.pageFetcher = pageFetcher;
        this.crawlerMapper = mapper;
    }

    /**
     * Crawls all pages up to {@link #getMaxPages()} and maps each raw listing to
     * the normalized domain model.
     * <p>
     * Store metadata (base URL, selectors, store name and status resolver) is
     * resolved once per crawl to avoid repeated allocations in the item loop.
     *
     * @return normalized listings collected from the target store.
     */
    @Override
    public List<StoreListing> crawlListings() {
        List<StoreListing> marketPriceStoreList = new ArrayList<>();
        final String baseUrl = storeBaseUrl();
        final StorePageSelectors pageSelectors = selectors();
        final StoreName storeName = store();
        final Function<String, Currency> currencyResolver = this::determineCurrency;
        final Function<String, ListingStatus> listingStatusResolver = this::calculateListingStatus;

        String url = baseUrl + getInitialSearchUrl();

        int pageCount = 0;
        while (url != null && pageCount < getMaxPages()) {
            pageCount++;

            String html = pageFetcher.fetch(url);
            if (html == null) {
                break;
            }

            Document doc = Jsoup.parse(html);
            Elements figurineItems = doc.select(pageSelectors.item());
            log.info("Found {} figurine items on page {}", figurineItems.size(), pageCount);

            figurineItems.forEach(element -> marketPriceStoreList.add(crawlerMapper
                    .toStoreListing(parseListing(element), storeName, currencyResolver, listingStatusResolver)));

            url = getNextPageUrl(doc, pageSelectors, baseUrl);
        }

        log.info("Finished retrieving store listing info for {}. Total pages: {}, Total items: {}", storeName,
                pageCount, marketPriceStoreList.size());

        return marketPriceStoreList;
    }

    /**
     * Parses a listing container element into a raw listing object.
     *
     * @param element
     *            listing container from the store page.
     * @return raw extracted listing fields.
     */
    protected abstract RawStoreListing parseListing(Element element);

    /**
     * @return absolute base URL for the target store.
     */
    protected abstract String storeBaseUrl();

    /**
     * @return initial path (relative or absolute) where crawling starts.
     */
    protected abstract String getInitialSearchUrl();

    /**
     * @return maximum number of pages to crawl.
     */
    protected abstract int getMaxPages();

    /**
     * @return CSS selectors used by this crawler implementation.
     */
    protected abstract StorePageSelectors selectors();

    /**
     * Maps raw price text to a normalized currency.
     *
     * @param priceText
     *            raw price text extracted from the store page.
     * @return normalized currency.
     */
    protected abstract Currency determineCurrency(String priceText);

    /**
     * Maps raw availability text to a normalized listing status.
     *
     * @param availabilityText
     *            availability text extracted from the store page.
     * @return normalized listing status.
     */
    protected abstract ListingStatus calculateListingStatus(String availabilityText);

    /**
     * Resolves the URL for the next page based on the configured selector.
     *
     * @param doc
     *            parsed document for the current page.
     * @param pageSelectors
     *            selectors configured for the current crawler implementation.
     * @param baseUrl
     *            absolute store base URL used to resolve relative next-page links.
     * @return next page URL, or {@code null} if no next page is available.
     */
    private String getNextPageUrl(Document doc, StorePageSelectors pageSelectors, String baseUrl) {
        Element nextPageLink = doc.selectFirst(pageSelectors.nextPage());
        if (nextPageLink != null) {
            String href = nextPageLink.attr("href");
            if (!href.isEmpty()) {
                if (href.startsWith("http")) {
                    return href;
                } else if (href.startsWith("/")) {
                    return baseUrl + href;
                }
            }
        }
        return null;
    }
}
