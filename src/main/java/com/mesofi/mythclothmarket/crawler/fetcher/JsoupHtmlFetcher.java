package com.mesofi.mythclothmarket.crawler.fetcher;

import java.io.IOException;

import org.jsoup.Jsoup;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsoupHtmlFetcher implements PageFetcher {

    @Override
    public String fetch(final String url) {
        try {
            return Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 "
                            + "(KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36")
                    .header("Accept-Language", "en-US,en;q=0.9").header("Upgrade-Insecure-Requests", "1").timeout(30000)
                    .get().html();
        } catch (IOException e) {
            log.error("Error fetching URL: {}", url, e);
            return null;
        }
    }
}
