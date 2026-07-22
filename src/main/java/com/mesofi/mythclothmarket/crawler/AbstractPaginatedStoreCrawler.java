package com.mesofi.mythclothmarket.crawler;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
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
import com.mesofi.mythclothmarket.crawler.model.LineUp;
import com.mesofi.mythclothmarket.crawler.model.LineUpDetection;
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
 * store-specific selectors, normalizing product names, and converting the
 * extracted information into normalized {@link StoreListing} instances.
 * <p>
 * Concrete subclasses are responsible only for providing store-specific
 * configuration such as the base URL, CSS selectors, lineup resolution,
 * currency resolution, listing status mapping, and any additional product name
 * normalization required by the target store.
 */
public abstract class AbstractPaginatedStoreCrawler implements StoreCrawler {

    private static final Logger log = LoggerFactory.getLogger(AbstractPaginatedStoreCrawler.class);

    // Add unnecessary words to remove for all the stores.
    private static final Set<String> KEYWORDS_TO_REMOVE = Set.of("");

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
        final StoreName store = store();
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

            figurineItems.forEach(item -> {
                RawStoreListing rawStoreListing = parseListing(item);
                rawStoreListing.setRawName(normalizeName(rawStoreListing.getRawName()));

                // Try to determine the lineup from the existing name to narrow the search.
                LineUpDetection lineUp = determineLineUp(rawStoreListing.getRawName());
                if (lineUp == null) {
                    throw new IllegalStateException("Provide a valid LineUpDetection");
                }

                rawStoreListing.setRawName(lineUp.normalizedName());

                StoreListing storeListing = crawlerMapper.toStoreListing(rawStoreListing, store, lineUp.lineUp(),
                        currencyResolver, listingStatusResolver);
                marketPriceStoreList.add(storeListing);
            });

            url = getNextPageUrl(doc, pageSelectors, baseUrl);
        }

        log.info("Finished retrieving store listing info for {}. Total pages: {}, Total items: {}", store, pageCount,
                marketPriceStoreList.size());

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
        StorePageSelectors selectors = selectors();

        extractAndSet(element, selectors.productName(), priceStore::setRawName);
        extractAndSet(element, selectors.productImage(), priceStore::setImageUrl);
        extractAndSet(element, selectors.productPrice(), priceStore::setPrice);
        extractAndSet(element, selectors.productUrl(), priceStore::setUrl);

        priceStore.setUrl(includeStoreBaseUrl() ? storeBaseUrl() + priceStore.getUrl() : priceStore.getUrl());

        Optional.ofNullable(selectors.discount())
                .ifPresent(selector -> extractAndSet(element, selector, priceStore::setDiscount));
        Optional.ofNullable(selectors.availability())
                .ifPresent(selector -> extractAndSet(element, selector, priceStore::setAvailability));

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
     * Extracts lineup information from the specified product name.
     * <p>
     * Implementations should apply any store-specific naming conventions to
     * determine the corresponding {@link LineUp} and return a
     * {@link LineUpDetection} containing both the detected lineup and the
     * normalized product name with any lineup prefix removed.
     *
     * @param nameText
     *            the raw product name to analyze
     * @return the result containing the detected lineup and normalized product name
     */
    protected abstract LineUpDetection determineLineUp(String nameText);

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
     * Normalizes a product name before it is mapped to a domain object.
     * <p>
     * This implementation removes common words that are not useful for identifying
     * a figurine, such as manufacturer or region names, and then delegates to
     * {@link #removeUnnecessaryWords(String)} so subclasses can perform
     * store-specific normalization.
     *
     * @param nameText
     *            the raw product name extracted from the store
     * @return the normalized product name
     */
    protected String normalizeName(final String nameText) {
        StringBuilder sb = new StringBuilder();
        for (String token : nameText.toLowerCase().split("\\s+")) {
            if (KEYWORDS_TO_REMOVE.contains(token)) {
                continue;
            }
            sb.append(token).append(" ");
        }

        return removeUnnecessaryWords(sb.toString().trim());
    }

    /**
     * Performs store-specific normalization of a product name.
     * <p>
     * Subclasses may override this method to remove or replace words that are
     * unique to a particular store's naming conventions. The default implementation
     * returns the supplied value unchanged.
     *
     * @param nameText
     *            the partially normalized product name
     * @return the normalized product name
     */
    protected String removeUnnecessaryWords(String nameText) {
        return nameText;
    }

    /**
     * Determines whether the store's base URL should be included in the listing
     * URLs.
     * <p>
     * Subclasses may override this method to include the base URL for stores that
     * require it. The default implementation returns {@code false}.
     *
     * @return {@code true} if the base URL should be included, {@code false}
     *         otherwise
     */
    protected boolean includeStoreBaseUrl() {
        return false;
    }

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
     * Extracts a value from the specified HTML element using the provided selector
     * and passes the extracted value to the supplied consumer.
     * <p>
     * If no element matches the selector, no action is performed.
     *
     * @param element
     *            the parent HTML element representing a product listing
     * @param selector
     *            the selector describing how to locate the child element and which
     *            value to extract
     * @param consumer
     *            the consumer that receives the extracted value
     */
    private void extractAndSet(Element element, ElementSelector selector, Consumer<String> consumer) {
        Optional.ofNullable(element.selectFirst(selector.selector()))
                .ifPresent(e -> consumer.accept(findElementValue(selector, e)));
    }

    /**
     * Extracts a value from the specified HTML element according to the supplied
     * selector.
     * <p>
     * If the selector defines an attribute, the corresponding attribute value is
     * returned. Otherwise, the element's visible text content is returned.
     *
     * @param elementSelector
     *            the selector describing which value to extract
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
