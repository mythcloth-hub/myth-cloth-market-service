package com.mesofi.mythclothmarket.crawler.fetcher;

import java.util.Map;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

public class PlaywrightHtmlFetcher implements PageFetcher {
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
            page.navigate(url);
            return page.content();
        }
    }
}
