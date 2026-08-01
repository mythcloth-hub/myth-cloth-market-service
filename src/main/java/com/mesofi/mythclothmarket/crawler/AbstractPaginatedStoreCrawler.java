package com.mesofi.mythclothmarket.crawler;

import java.net.URI;
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
import com.mesofi.mythclothmarket.crawler.model.LineUpDetection;
import com.mesofi.mythclothmarket.crawler.model.LineUpMatcher;
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
 * Concrete subclasses provide store-specific configuration via template methods
 * such as {@link #storeBaseUrl()}, {@link #getInitialSearchUrl()},
 * {@link #selectors()}, {@link #getLineUpMatchers()},
 * {@link #determineCurrency(String)}, and
 * {@link #calculateListingStatus(String)}.
 */
public abstract class AbstractPaginatedStoreCrawler implements StoreCrawler {

    private static final Logger log = LoggerFactory.getLogger(AbstractPaginatedStoreCrawler.class);

    // Add unnecessary words to remove for all the stores.
    private static final Set<String> KEYWORDS_TO_REMOVE = Set.of("\"", "/", "・");

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
        final URI baseUrl = storeBaseUrl();
        final StorePageSelectors pageSelectors = selectors();
        final StoreName store = store();
        final Function<String, Currency> currencyResolver = this::determineCurrency;
        final Function<String, ListingStatus> listingStatusResolver = this::calculateListingStatus;
        final Function<Element, Boolean> preorderResolver = this::calculatePreorder;

        URI url = baseUrl.resolve(getInitialSearchUrl());
        log.info("Retrieving prices using the initial URL: {}", url);
        if (getMaxPages() == 0) {
            throw new IllegalArgumentException("Unable to retrieve prices, max pages must be greater than zero");
        }

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

                // Normalize extracted fields and adjust image URLs before mapping.
                rawStoreListing.setNormalizedName(normalizeName(rawStoreListing.getOriginalName()));
                if (prependedStoreBaseUrlInProductUrl()) {
                    rawStoreListing.setProductUrl(storeBaseUrl().resolve(rawStoreListing.getProductUrl()).toString());
                }
                if (prependedStoreBaseUrlInImageUrl()) {
                    rawStoreListing.setImageUrl(storeBaseUrl().resolve(rawStoreListing.getImageUrl()).toString());
                }

                rawStoreListing.setImageUrl(filterImageUrl(rawStoreListing.getImageUrl()));

                // Try to determine the lineup from the existing name to narrow the search.
                LineUpDetection lineUp = determineLineUp(rawStoreListing.getNormalizedName());
                if (lineUp == null) {
                    throw new IllegalStateException("Provide a valid LineUpDetection");
                }

                rawStoreListing.setNormalizedName(lineUp.normalizedName());

                StoreListing storeListing = crawlerMapper.toStoreListing(rawStoreListing, store, lineUp.lineUp(),
                        currencyResolver, listingStatusResolver, preorderResolver);

                marketPriceStoreList.add(storeListing);
            });

            url = getNextPageUrl(doc, pageSelectors.nextPage(), baseUrl);
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

        extractAndSet(element, selectors.productName(), priceStore::setOriginalName);
        extractAndSet(element, selectors.productName(), priceStore::setNormalizedName);
        extractAndSet(element, selectors.productImage(), priceStore::setImageUrl);
        extractAndSet(element, selectors.productUrl(), priceStore::setProductUrl);
        extractAndSet(element, selectors.productPrice(), priceStore::setPriceText);

        Optional.ofNullable(selectors.discount())
                .ifPresent(selector -> extractAndSet(element, selector, priceStore::setDiscountText));
        Optional.ofNullable(selectors.availability())
                .ifPresent(selector -> extractAndSet(element, selector, priceStore::setAvailabilityText));
        Optional.ofNullable(selectors.preorder())
                .ifPresent(selector -> priceStore.setPreorderElement(element.selectFirst(selector)));

        return priceStore;
    }

    /**
     * Returns the absolute base URL for the target store.
     *
     * @return absolute base URL for the target store
     */
    protected abstract URI storeBaseUrl();

    /**
     * Returns the initial search URL used to start crawling.
     * <p>
     * The returned value may be relative to {@link #storeBaseUrl()} or absolute.
     *
     * @return initial path (relative or absolute) where crawling starts
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
    protected LineUpDetection determineLineUp(String nameText) {
        for (LineUpMatcher matcher : getLineUpMatchers()) {
            if (matcher.matches(nameText)) {
                return new LineUpDetection(matcher.lineUp(), matcher.extractProductName(nameText));
            }
        }
        return new LineUpDetection(null, nameText);
    }

    /**
     * Returns the ordered lineup matchers used by the default lineup detection
     * algorithm.
     * <p>
     * Matchers are evaluated in declaration order; therefore, more specific aliases
     * should be placed before broader aliases.
     *
     * @return ordered lineup matchers for the target store
     */
    protected abstract List<LineUpMatcher> getLineUpMatchers();

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
     * Determines whether a listing is available for preorder. This method should be
     * overridden by subclasses that need to detect preorder status based on
     * specific HTML elements or attributes.
     *
     * @param preorderElement
     *            the raw preorder element extracted from the listing
     * @return {@code true} if the listing is available for preorder, {@code false}
     *         otherwise
     */
    protected Boolean calculatePreorder(Element preorderElement) {
        return preorderElement != null;
    }

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
     * Performs store-specific filtering of an image URL.
     * <p>
     * Subclasses may override this method to modify or clean up image URLs
     * according to the store's specific requirements. The default implementation
     * returns the supplied value unchanged.
     *
     * @param imageUrl
     *            the raw image URL extracted from the store
     * @return the filtered image URL
     */
    protected String filterImageUrl(String imageUrl) {
        return imageUrl;
    }

    /**
     * Determines whether the store's base URL should be prepended to extracted
     * product URLs.
     * <p>
     * Some stores expose product URLs as relative paths, requiring the store's base
     * URL to construct absolute product URLs. Subclasses may override this method
     * to indicate that the base URL should be included. The default implementation
     * returns {@code false}.
     * </p>
     *
     * @return {@code true} if the store's base URL should be prepended to product
     *         URLs; {@code false} otherwise
     */
    protected boolean prependedStoreBaseUrlInProductUrl() {
        return false;
    }

    /**
     * Determines whether the store's base URL should be prepended to extracted
     * image URLs.
     * <p>
     * Some stores expose image URLs as relative paths, requiring the store's base
     * URL to construct absolute image URLs. Subclasses may override this method to
     * indicate that the base URL should be included. The default implementation
     * returns {@code false}.
     * </p>
     *
     * @return {@code true} if the store's base URL should be prepended to image
     *         URLs; {@code false} otherwise
     */
    protected boolean prependedStoreBaseUrlInImageUrl() {
        return false;
    }

    /**
     * Resolves the URL of the next listing page.
     *
     * @param doc
     *            the parsed HTML document of the current page
     * @param nextPageSelector
     *            the CSS selector for the next page link
     * @param baseUrl
     *            the store's base URL used to resolve relative links
     * @return the next page URL, or {@code null} if no additional page exists
     */
    private URI getNextPageUrl(Document doc, String nextPageSelector, URI baseUrl) {
        if (nextPageSelector == null) {
            return null;
        }

        Element nextPageLink = doc.selectFirst(nextPageSelector);
        if (nextPageLink != null) {
            String href = nextPageLink.attr("href");
            if (!href.isEmpty()) {
                if (href.startsWith("http")) {
                    return URI.create(href);
                } else if (href.startsWith("/")) {
                    return baseUrl.resolve(href);
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
        boolean multiple = selector.multiple();
        if (multiple) {
            Elements elements = element.select(selector.selector());
            String valueElement = elements.stream().map(theElement -> findElementValue(selector, theElement))
                    .map(String::trim).filter(value -> !value.isEmpty()).findFirst().orElse(null);
            consumer.accept(valueElement);
        } else {
            Optional.ofNullable(element.selectFirst(selector.selector()))
                    .ifPresent(e -> consumer.accept(findElementValue(selector, e)));
        }
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
