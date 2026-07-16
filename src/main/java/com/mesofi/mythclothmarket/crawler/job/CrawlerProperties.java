package com.mesofi.mythclothmarket.crawler.job;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.mesofi.mythclothmarket.crawler.model.StoreName;

/**
 * Configuration properties for store crawler scheduling.
 * <p>
 * Binds the configuration defined under the
 * {@code myth-cloth-market-service.crawler} prefix, providing the list of
 * crawler jobs that should be registered with Quartz at application startup.
 * Each job defines the target store, scheduling expression, and whether the job
 * is enabled.
 *
 * @param jobs
 *            the configured crawler jobs
 */
@ConfigurationProperties(prefix = "myth-cloth-market-service.crawler")
public record CrawlerProperties(List<Job> jobs) {
    /**
     * Represents the scheduling configuration for a single store crawler.
     *
     * @param key
     *            the unique identifier of the store to be crawled
     * @param name
     *            the descriptive name of the crawler job
     * @param cron
     *            the Quartz cron expression that defines the execution schedule
     * @param enabled
     *            whether the crawler job should be scheduled at application startup
     */
    public record Job(StoreName key, String name, String cron, boolean enabled) {
    }
}
