package com.mesofi.mythclothmarket.job;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.mesofi.mythclothmarket.crawler.model.StoreName;

@ConfigurationProperties(prefix = "myth-cloth-market-service.crawler")
public record CrawlerProperties(List<Job> jobs) {

    public record Job(StoreName key, String name, String cron, boolean enabled) {
    }
}
