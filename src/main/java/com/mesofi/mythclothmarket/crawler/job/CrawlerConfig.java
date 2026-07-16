package com.mesofi.mythclothmarket.crawler.job;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registers crawler-related Spring configuration.
 * <p>
 * Enables binding of the external crawler scheduling configuration defined in
 * {@link CrawlerProperties}, allowing store crawler jobs to be configured
 * through the application's configuration files.
 */
@Configuration
@EnableConfigurationProperties(CrawlerProperties.class)
public class CrawlerConfig {
}
