package com.mesofi.mythclothmarket.crawler.fetcher;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Jsoup-backed implementation of {@link PageFetcher} used to retrieve
 * storefront HTML while preserving browser-like session state.
 * <p>
 * This fetcher is intentionally tolerant of storefront-specific request flows:
 * it can perform a simple GET, or it can first establish a baseline session,
 * execute a custom request (for example a currency switch POST), and then fetch
 * the target page with the merged cookies. This makes it suitable for stores
 * that keep locale or currency state in session cookies rather than in the URL.
 * <p>
 * The component also detects likely bot challenge pages and avoids returning
 * their HTML to the crawler pipeline.
 */
@Slf4j
@Component
public class JsoupHtmlFetcher implements PageFetcher {

    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36";

    /**
     * Fetches HTML using a browser-like Jsoup session while preserving any custom
     * request state required by the storefront.
     * <p>
     * When a custom request is provided, the fetcher establishes a baseline
     * session, executes the request (for example a POST that selects the desired
     * storefront currency), merges the resulting cookies, and performs the final
     * page load with the same session state. This is necessary for sites such as
     * Nin-Nin-Game that keep the selected currency in cookies rather than in the
     * URL itself.
     *
     * @param request
     *            a function that configures and executes a custom request and
     *            returns the associated {@link Connection.Response}
     * @param url
     *            absolute page URL
     * @return HTML content, or {@code null} when the request fails
     */
    @Override
    public String fetch(Function<Connection, Connection.Response> request, final URI url) {
        Document document;
        try {
            Connection connection = baseConnection(url.toString());
            if (request != null) {
                Connection.Response initialResponse = connection.execute();
                Connection.Response requestResponse = request
                        .apply(baseConnection(url.toString()).cookies(initialResponse.cookies()));
                document = baseConnection(url.toString()).cookies(mergeCookies(initialResponse, requestResponse)).get();
            } else {
                document = connection.get();
            }

            String title = document.title();
            Element body = document.body();
            String bodyText = body.text();

            if (isBlockedPage(title, bodyText)) {
                throw new BlockedPageException("Blocked by anti-bot challenge while fetching URL: " + url);
            }

            return document.html();
        } catch (IOException e) {
            log.error("Error fetching URL: {}", url, e);
            return null;
        }
    }

    /**
     * Merges cookies from the initial session and the custom request so the final
     * page load uses the same state as the storefront's browser flow.
     *
     * @param initialResponse
     *            the response from the baseline page request
     * @param requestResponse
     *            the response from the custom pre-flight request
     * @return the combined cookie map used for the final fetch
     */
    private Map<String, String> mergeCookies(Connection.Response initialResponse, Connection.Response requestResponse) {
        Map<String, String> cookies = new HashMap<>(initialResponse.cookies());
        cookies.putAll(requestResponse.cookies());
        return cookies;
    }

    /**
     * Builds a reusable Jsoup connection configured with browser-like headers for
     * storefront requests.
     *
     * @param url
     *            the page URL to request
     * @return a configured {@link Connection} instance
     */
    protected Connection baseConnection(String url) {
        return Jsoup.connect(url).userAgent(USER_AGENT).header("Accept-Language", "en-US,en;q=0.9")
                .header("Upgrade-Insecure-Requests", "1").timeout(30000);
    }

    /**
     * Detects bot challenge pages that should be treated as blocked instead of
     * passed along to the crawler.
     *
     * @param title
     *            the page title, or {@code null} when absent
     * @param bodyText
     *            the visible body text, or {@code null} when absent
     * @return {@code true} when the response appears to be an anti-bot challenge
     */
    private boolean isBlockedPage(String title, String bodyText) {
        String normalizedTitle = title == null ? "" : title.toLowerCase();
        String normalizedBody = bodyText == null ? "" : bodyText.toLowerCase();

        return normalizedTitle.contains("challenge") || normalizedBody.contains("we're confirming that you're human")
                || normalizedBody.contains("enable javascript and cookies to continue");
    }
}
