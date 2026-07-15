package com.mesofi.mythclothmarket;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

public class NinNinGameFigurinesExample {
    private static final String CATEGORY_URL = "https://www.nin-nin-game.com/en/myth-cloth-saint-seiya";
    private static final String PRODUCT_NAME_SELECTOR = "a.product-name";
    private static final String NEXT_PAGE_SELECTOR = "#pagination_next_bottom a";

    public static void main(String[] args) {
        try (Playwright playwright = Playwright.create();
                Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false))) {

            Page page = browser.newPage();
            page.navigate(CATEGORY_URL);
            page.waitForLoadState();
            page.locator(PRODUCT_NAME_SELECTOR).first().waitFor();

            Set<String> figurineNames = new LinkedHashSet<>();

            while (true) {
                List<String> namesOnPage = page.locator(PRODUCT_NAME_SELECTOR).allTextContents();
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
                page.locator(PRODUCT_NAME_SELECTOR).first().waitFor();
            }

            figurineNames.forEach(System.out::println);
            System.out.println("Total figurines: " + figurineNames.size());
        }
    }
}
