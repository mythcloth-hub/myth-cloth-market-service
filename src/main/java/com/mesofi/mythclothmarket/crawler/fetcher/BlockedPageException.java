package com.mesofi.mythclothmarket.crawler.fetcher;

/**
 * Thrown when a fetcher detects an anti-bot or challenge page instead of the
 * expected store HTML.
 */
public class BlockedPageException extends RuntimeException {

    private static final long serialVersionUID = -1867676152308300900L;

    public BlockedPageException(String message) {
        super(message);
    }
}
