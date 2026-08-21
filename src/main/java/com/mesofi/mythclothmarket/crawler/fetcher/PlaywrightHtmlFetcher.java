package com.mesofi.mythclothmarket.crawler.fetcher;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jsoup.Connection;
import org.springframework.stereotype.Component;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import lombok.extern.slf4j.Slf4j;

/**
 * Playwright-backed implementation of {@link PageFetcher} for storefront pages
 * that require a real browser environment to render correctly.
 * <p>
 * This fetcher starts a headless browser context with browser-like headers,
 * viewport settings, locale, and timezone, then navigates to the requested URL.
 * It is primarily used for pages that perform client-side rendering or require
 * browser state to avoid anti-bot protections. For Mandarake order pages, it
 * also performs a warmup navigation before the actual target page is loaded.
 */
@Slf4j
@Component
public class PlaywrightHtmlFetcher implements PageFetcher {
    private static final String MANDARAKE_ORDER_HOST = "order.mandarake.co.jp";
    private static final String MANDARAKE_WARMUP_URL = "https://www.mandarake.co.jp/index2.html";
    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36";

    private final PlaywrightProperties playwrightProperties;

    public PlaywrightHtmlFetcher(PlaywrightProperties playwrightProperties) {
        this.playwrightProperties = playwrightProperties;
    }

    /**
     * Fetches the fully rendered HTML for the requested page by opening it in a
     * headless browser context configured to behave like a normal browser session.
     * <p>
     * The request hook parameter is accepted for interface compatibility with the
     * fetcher contract, but this implementation does not use it because the browser
     * navigation flow is managed directly by Playwright. For Mandarake order pages,
     * a pre-flight warmup navigation is executed before the final page load to
     * reduce bot-detection and redirect issues.
     *
     * @param request
     *            unused compatibility argument for the generic fetch contract
     * @param url
     *            absolute page URL to load in the browser
     * @return rendered page HTML content, or {@code null} when the load fails
     */
    @Override
    public String fetch(Function<Connection, Connection.Response> request, final URI url) {
        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                .setHeadless(playwrightProperties.headless()).setArgs(defaultedArgs(playwrightProperties.args()));

        if (hasText(playwrightProperties.channel())) {
            launchOptions.setChannel(playwrightProperties.channel());
        }

        if (hasText(playwrightProperties.executablePath())) {
            launchOptions.setExecutablePath(Path.of(playwrightProperties.executablePath()));
        }

        try (Playwright playwright = Playwright.create();
                Browser browser = playwright.chromium().launch(launchOptions)) {
            BrowserContext context = browser.newContext(new Browser.NewContextOptions().setUserAgent(USER_AGENT)
                    .setViewportSize(1920, 1080).setLocale("en-US").setTimezoneId("America/Mexico_City"));

            context.setExtraHTTPHeaders(Map.of("Accept-Language", "en-US,en;q=0.9", "Upgrade-Insecure-Requests", "1"));

            Page page = context.newPage();

            if (requiresMandarakeWarmup(url)) {
                page.navigate(MANDARAKE_WARMUP_URL);
                page.waitForLoadState();
                page.waitForTimeout(1_500);
            }

            page.navigate(url.toString());

            if (requiresMandarakeWarmup(url) && page.url().startsWith("https://www.mandarake.co.jp/")) {
                throw new BlockedPageException("Mandarake order page redirected to home page: " + url);
            }

            return page.content();
        }
    }

    private boolean requiresMandarakeWarmup(URI url) {
        return MANDARAKE_ORDER_HOST.equalsIgnoreCase(url.getHost());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private List<String> defaultedArgs(List<String> args) {
        return args == null ? List.of() : args;
    }
}
