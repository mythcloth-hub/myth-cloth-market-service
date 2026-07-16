package com.mesofi.mythclothmarket.job;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CrawlerProperties.class)
public class CrawlerConfig {
}
