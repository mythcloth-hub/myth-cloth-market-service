package com.mesofi.mythclothmarket.crawler.fetcher;

import java.net.URI;

/**
 * Fetches raw HTML content for a given URL.
 */
public interface PageFetcher {

    /**
     * @param url
     *            absolute page URL.
     * @return HTML content, or {@code null} when the page cannot be fetched.
     */
    String fetch(URI url);
}
