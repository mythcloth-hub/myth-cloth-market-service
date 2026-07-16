package com.mesofi.mythclothmarket.crawler.fetcher;

/**
 * Thrown when a fetcher detects an anti-bot or challenge page instead of the
 * expected store HTML.
 */
public class BlockedPageException extends RuntimeException {

    public BlockedPageException(String message) {
        super(message);
    }
}
