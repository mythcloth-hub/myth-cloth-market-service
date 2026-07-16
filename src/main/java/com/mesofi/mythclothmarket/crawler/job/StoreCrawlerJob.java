package com.mesofi.mythclothmarket.crawler.job;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import com.mesofi.mythclothmarket.crawler.StoreCrawler;
import com.mesofi.mythclothmarket.pricing.MarketPricingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Quartz job responsible for triggering the synchronization of market listings
 * for a specific online retailer.
 * <p>
 * The job obtains the required {@link MarketPricingService} and
 * {@link StoreCrawler} instances from the Quartz {@code JobDataMap} and
 * delegates the synchronization process to the service layer.
 * <p>
 * This class intentionally contains no business logic; its sole responsibility
 * is to bridge Quartz scheduling with the application's market synchronization
 * workflow.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StoreCrawlerJob implements Job {

    /**
     * Executes the scheduled synchronization for the configured store.
     * <p>
     * The execution retrieves the required dependencies from the Quartz
     * {@link org.quartz.JobDataMap}, invokes the market synchronization service,
     * and logs the start and completion of the job.
     *
     * @param context
     *            the Quartz execution context containing the configured job details
     *            and associated data
     */
    @Override
    public void execute(JobExecutionContext context) {
        // Extract both instances dynamically from the JobDataMap
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();

        MarketPricingService marketPricingService = (MarketPricingService) dataMap.get("marketService");
        StoreCrawler storeCrawler = (StoreCrawler) dataMap.get("storeCrawler");

        log.info("Quartz Job triggered for store: {} ...", storeCrawler.store());

        // delegates to the service layer ...
        marketPricingService.synchronizeStoreListings(storeCrawler);

        log.info("Quartz Job finished for store: {} ...", storeCrawler.store());
    }
}
