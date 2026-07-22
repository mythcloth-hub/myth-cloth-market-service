package com.mesofi.mythclothmarket.crawler.fetcher;

import java.net.URI;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import lombok.extern.slf4j.Slf4j;

/**
 * {@link PageFetcher} backed by a headless Playwright browser.
 */
@Slf4j
@Component
public class PlaywrightHtmlFetcher implements PageFetcher {
    private static final String MANDARAKE_ORDER_HOST = "order.mandarake.co.jp";
    private static final String MANDARAKE_WARMUP_URL = "https://www.mandarake.co.jp/index2.html";

    /**
     * Fetches fully rendered HTML by navigating the page in a headless browser.
     *
     * @param url
     *            absolute page URL.
     * @return rendered page HTML.
     */
    @Override
    public String fetch(String url) {

        try (Playwright playwright = Playwright.create();
                Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))) {
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 "
                            + "(KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36")
                    .setViewportSize(1920, 1080).setLocale("en-US").setTimezoneId("America/Mexico_City"));

            context.setExtraHTTPHeaders(Map.of("Accept-Language", "en-US,en;q=0.9", "Upgrade-Insecure-Requests", "1"));

            Page page = context.newPage();

            if (requiresMandarakeWarmup(url)) {
                page.navigate(MANDARAKE_WARMUP_URL);
                page.waitForLoadState();
                page.waitForTimeout(1_500);
            }

            page.navigate(url);

            if (requiresMandarakeWarmup(url) && page.url().startsWith("https://www.mandarake.co.jp/")) {
                throw new BlockedPageException("Mandarake order page redirected to home page: " + url);
            }

            return page.content();
        }
    }

    private boolean requiresMandarakeWarmup(String url) {
        return MANDARAKE_ORDER_HOST.equalsIgnoreCase(URI.create(url).getHost());
    }
}
