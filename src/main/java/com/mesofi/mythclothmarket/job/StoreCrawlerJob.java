package com.mesofi.mythclothmarket.job;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import com.mesofi.mythclothmarket.MarketService;
import com.mesofi.mythclothmarket.crawler.StoreCrawler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class StoreCrawlerJob implements Job {

    @Override
    public void execute(JobExecutionContext context) {
        // Extract both instances dynamically from the JobDataMap
        MarketService marketService = (MarketService) context.getJobDetail().getJobDataMap().get("marketService");
        StoreCrawler storeCrawler = (StoreCrawler) context.getJobDetail().getJobDataMap().get("storeCrawler");

        log.info("Quartz Job triggered for store: {} ...", storeCrawler.store());

        // delegates to the service layer ...
        marketService.retrieveAndPublishPrices(storeCrawler);

        log.info("Quartz Job finished for store: {} ...", storeCrawler.store());
    }
}
