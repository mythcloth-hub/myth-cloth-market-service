package com.mesofi.mythclothmarket.example;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitForSelectorState;

/**
 * Standalone Playwright example that prints figurine names from Nin-Nin-Game.
 */
public class NinNinGameFigurinesExample {
    private static final String CATEGORY_URL = "https://www.nin-nin-game.com/en/myth-cloth-saint-seiya";
    private static final String PRODUCT_NAME_SELECTOR = "a.product-name";
    private static final String NEXT_PAGE_SELECTOR = "#pagination_next_bottom a";

    /**
     * Runs the Playwright-based pagination scraper and prints all discovered
     * figurine names.
     *
     * @param args
     *            unused command-line arguments.
     */
    public static void main(String[] args) {

        System.out.println("Initializing Playwright with Nin-Nin-Game");

        try (Playwright playwright = Playwright.create();
                Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))) {

            System.out.println("Nin-Nin-Game started");

            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 "
                            + "(KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36")
                    .setViewportSize(1920, 1080).setLocale("en-US").setTimezoneId("America/Mexico_City"));

            context.setExtraHTTPHeaders(Map.of("Accept-Language", "en-US,en;q=0.9", "Upgrade-Insecure-Requests", "1"));

            Page page = context.newPage();
            // Page page = browser.newPage();

            System.out.println("After the page ...");
            page.navigate(CATEGORY_URL);
            System.out.println("Nin-Nin-Game navigated to: " + CATEGORY_URL);
            page.waitForLoadState();
            System.out.println("After the navigation ...");

            System.out.println(page.content());

            page.locator(PRODUCT_NAME_SELECTOR).first().waitFor(new com.microsoft.playwright.Locator.WaitForOptions()
                    .setState(WaitForSelectorState.ATTACHED).setTimeout(15_000));
            System.out.println("-----");

            Set<String> figurineNames = new LinkedHashSet<>();

            while (true) {
                List<String> namesOnPage = page.locator(PRODUCT_NAME_SELECTOR).allTextContents();
                System.out.println(namesOnPage);

                for (String name : namesOnPage) {
                    String trimmed = name.trim();
                    if (!trimmed.isEmpty()) {
                        figurineNames.add(trimmed);
                    }
                }

                if (page.locator(NEXT_PAGE_SELECTOR).count() == 0) {
                    break;
                }

                page.locator(NEXT_PAGE_SELECTOR).click();
                page.waitForLoadState();
                page.locator(PRODUCT_NAME_SELECTOR).first().waitFor(
                        new com.microsoft.playwright.Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED));
            }

            figurineNames.forEach(System.out::println);
            System.out.println("Total figurines: " + figurineNames.size());
        }
    }
}
