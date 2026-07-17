package com.mesofi.mythclothmarket.crawler;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
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
import com.mesofi.mythclothmarket.crawler.model.ElementSelector;
import com.mesofi.mythclothmarket.crawler.model.ListingStatus;
import com.mesofi.mythclothmarket.crawler.model.StoreListing;
import com.mesofi.mythclothmarket.crawler.model.StoreName;
import com.mesofi.mythclothmarket.crawler.model.StorePageSelectors;

/**
 * Base implementation of {@link StoreCrawler} for online stores that expose
 * product listings across paginated HTML pages.
 * <p>
 * This class encapsulates the common crawling workflow, including retrieving
 * HTML pages, traversing pagination, extracting raw listing data using
 * store-specific selectors, and converting the extracted information into
 * normalized {@link StoreListing} instances.
 * <p>
 * Concrete subclasses are responsible only for providing store-specific
 * configuration such as the base URL, CSS selectors, currency resolution, and
 * listing status mapping.
 */
public abstract class AbstractPaginatedStoreCrawler implements StoreCrawler {

    Logger log = LoggerFactory.getLogger(AbstractPaginatedStoreCrawler.class);

    private final PageFetcher pageFetcher;
    private final CrawlerMapper crawlerMapper;

    /**
     * Creates a paginated store crawler.
     *
     * @param pageFetcher
     *            the component responsible for retrieving HTML pages
     * @param mapper
     *            the mapper that converts raw scraped values into normalized
     *            {@link StoreListing} instances
     */
    protected AbstractPaginatedStoreCrawler(PageFetcher pageFetcher, CrawlerMapper mapper) {
        this.pageFetcher = pageFetcher;
        this.crawlerMapper = mapper;
    }

    /**
     * Crawls all configured listing pages for the target store.
     * <p>
     * Starting from the initial search URL, this method retrieves each page,
     * extracts every product listing, converts the raw scraped values into
     * normalized {@link StoreListing} instances, and continues following the
     * pagination links until no additional pages are available or the configured
     * page limit is reached.
     *
     * @return the list of normalized store listings retrieved from the target store
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
            Elements figurineItems = doc.select(pageSelectors.listingContainer());
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
     * Extracts the raw values from a single listing element.
     * <p>
     * The extraction process is driven entirely by the configured
     * {@link StorePageSelectors}. The returned {@link RawStoreListing} contains the
     * values exactly as they appear on the page, leaving normalization to the
     * mapping layer.
     *
     * @param element
     *            the HTML element representing a single product listing
     * @return the extracted raw listing information
     */
    protected RawStoreListing parseListing(Element element) {
        RawStoreListing priceStore = new RawStoreListing();

        // rawName
        ElementSelector productNameSelector = selectors().productName();
        Optional.ofNullable(element.selectFirst(productNameSelector.selector()))
                .ifPresent(e -> priceStore.setRawName(findElementValue(productNameSelector, e)));

        // imageUrl
        ElementSelector productImageSelector = selectors().productImage();
        Optional.ofNullable(element.selectFirst(productImageSelector.selector()))
                .ifPresent(e -> priceStore.setImageUrl(findElementValue(productImageSelector, e)));

        // url
        ElementSelector productUrlSelector = selectors().productUrl();
        Optional.ofNullable(element.selectFirst(productUrlSelector.selector()))
                .ifPresent(e -> priceStore.setUrl(findElementValue(productUrlSelector, e)));

        // price
        ElementSelector productPriceSelector = selectors().productPrice();
        Optional.ofNullable(element.selectFirst(productPriceSelector.selector()))
                .ifPresent(e -> priceStore.setPrice(findElementValue(productPriceSelector, e)));

        // discount
        ElementSelector discountSelector = selectors().discount();
        Optional.ofNullable(discountSelector)
                .flatMap(eSelector -> Optional.ofNullable(element.selectFirst(eSelector.selector())))
                .ifPresent(e -> priceStore.setDiscount(findElementValue(discountSelector, e)));

        // availability
        ElementSelector availabilitySelector = selectors().availability();
        Optional.ofNullable(availabilitySelector)
                .flatMap(eSelector -> Optional.ofNullable(element.selectFirst(eSelector.selector())))
                .ifPresent(e -> priceStore.setAvailability(findElementValue(availabilitySelector, e)));

        return priceStore;
    }

    /**
     * @return absolute base URL for the target store.
     */
    protected abstract String storeBaseUrl();

    /**
     * @return initial path (relative or absolute) where crawling starts.
     */
    protected abstract String getInitialSearchUrl();

    /**
     * Returns the maximum number of listing pages that should be crawled.
     *
     * @return the maximum number of pages to visit
     */
    protected abstract int getMaxPages();

    /**
     * Returns the CSS selectors required to extract listing information from the
     * target store.
     *
     * @return the configured page selectors
     */
    protected abstract StorePageSelectors selectors();

    /**
     * Determines the currency associated with a listing.
     * <p>
     * Implementations may infer the currency from the raw price text or return a
     * fixed currency for stores that always use the same one.
     *
     * @param priceText
     *            the raw price text extracted from the listing
     * @return the resolved currency, or {@code null} if it cannot be determined
     */
    protected abstract Currency determineCurrency(String priceText);

    /**
     * Converts the store-specific availability information into a normalized
     * {@link ListingStatus}.
     *
     * @param availabilityText
     *            the raw availability text extracted from the listing
     * @return the corresponding listing status, or {@code null} if it cannot be
     *         determined
     */
    protected abstract ListingStatus calculateListingStatus(String availabilityText);

    /**
     * Resolves the URL of the next listing page.
     *
     * @param doc
     *            the parsed HTML document of the current page
     * @param pageSelectors
     *            the selectors configured for the current store
     * @param baseUrl
     *            the store's base URL used to resolve relative links
     * @return the next page URL, or {@code null} if no additional page exists
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

    /**
     * Extracts the value represented by the given selector.
     * <p>
     * If the selector specifies an attribute, the corresponding attribute value is
     * returned. Otherwise, the element's visible text content is returned.
     *
     * @param elementSelector
     *            the selector describing how to extract the value
     * @param theElement
     *            the matched HTML element
     * @return the extracted and trimmed value
     */
    private String findElementValue(ElementSelector elementSelector, Element theElement) {
        return elementSelector.attribute() == null
                ? theElement.text().trim()
                : theElement.attr(elementSelector.attribute()).trim();
    }
}
