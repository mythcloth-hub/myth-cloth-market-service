package com.mesofi.mythclothmarket.crawler.fetcher;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for Playwright browser launch behavior.
 *
 * @param headless
 *            whether the browser should run headless
 * @param channel
 *            browser channel (for example: chrome, msedge)
 * @param executablePath
 *            absolute browser executable path
 * @param args
 *            additional browser process arguments
 */
@ConfigurationProperties(prefix = "myth-cloth-market-service.crawler.playwright")
public record PlaywrightProperties(boolean headless, String channel, String executablePath, List<String> args) {
}
