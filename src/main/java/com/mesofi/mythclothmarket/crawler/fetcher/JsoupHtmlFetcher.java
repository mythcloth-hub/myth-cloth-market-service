package com.mesofi.mythclothmarket.crawler.fetcher;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * {@link PageFetcher} backed by Jsoup HTTP requests.
 */
@Slf4j
@Component
public class JsoupHtmlFetcher implements PageFetcher {

    /**
     * Fetches HTML with browser-like headers to reduce anti-bot blocking.
     *
     * @param url
     *            absolute page URL.
     * @return HTML content, or {@code null} when the request fails.
     */
    @Override
    public String fetch(final String url) {
        try {
            Document document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 "
                            + "(KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36")
                    .header("Accept-Language", "en-US,en;q=0.9").header("Upgrade-Insecure-Requests", "1").timeout(30000)
                    .get();

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

    private boolean isBlockedPage(String title, String bodyText) {
        String normalizedTitle = title == null ? "" : title.toLowerCase();
        String normalizedBody = bodyText == null ? "" : bodyText.toLowerCase();

        return normalizedTitle.contains("challenge") || normalizedBody.contains("we're confirming that you're human")
                || normalizedBody.contains("enable javascript and cookies to continue");
    }
}
