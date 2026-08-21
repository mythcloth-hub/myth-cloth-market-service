
package com.mesofi.mythclothmarket.crawler.fetcher;

import java.net.URI;
import java.util.function.Function;

import org.jsoup.Connection;
import org.jsoup.Connection.Response;

/**
 * Fetches raw HTML content from web pages.
 *
 * <p>
 * A {@code PageFetcher} provides a simple way to retrieve the HTML content of a
 * page, either by using a default request configuration or by supplying a
 * custom request operation.
 * </p>
 */
public interface PageFetcher {

    /**
     * Fetches the HTML content of the page using a custom request operation.
     *
     * <p>
     * The supplied function receives a Jsoup {@link Connection} and is responsible
     * for configuring and executing the HTTP request. This can be used when a page
     * requires a request other than the default GET request, such as a POST request
     * with form data.
     * </p>
     *
     * @param request
     *            function that configures and executes the HTTP request, returning
     *            the resulting {@link Response}.
     * @param url
     *            absolute page URL.
     * @return HTML content, or {@code null} when the page cannot be fetched.
     */
    String fetch(Function<Connection, Response> request, URI url);
}
