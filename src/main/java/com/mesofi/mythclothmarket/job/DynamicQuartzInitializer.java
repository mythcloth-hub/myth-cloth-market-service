package com.mesofi.mythclothmarket.job;

import java.util.List;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Configuration;

import com.mesofi.mythclothmarket.MarketService;
import com.mesofi.mythclothmarket.crawler.StoreCrawler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DynamicQuartzInitializer implements SmartInitializingSingleton {

    private final List<StoreCrawler> crawlers;
    private final CrawlerProperties crawlerProperties;
    private final MarketService marketService;
    private final Scheduler scheduler;

    @Override
    public void afterSingletonsInstantiated() {
        log.info("Initializing dynamic Quartz jobs for store crawlers...");
        // Here you can add logic to dynamically register jobs based on your
        // configuration

        for (StoreCrawler crawler : crawlers) {

            CrawlerProperties.Job jobConfig = crawlerProperties.jobs().stream()
                    .filter(job -> job.key() == crawler.store()).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No cron expression found for store: " + crawler.store()));

            String storeKey = crawler.store().name();

            try {
                if (!jobConfig.enabled()) {
                    log.info("Crawler job '{}' is disabled, skipping scheduling.", jobConfig.name());
                    continue;
                }

                // 1. Pack dependencies into the JobDataMap
                JobDataMap dataMap = new JobDataMap();
                dataMap.put("marketService", marketService);
                dataMap.put("storeCrawler", crawler);

                // 2. Build unique Job Detail using the store key
                JobDetail jobDetail = JobBuilder.newJob(StoreCrawlerJob.class)
                        .withIdentity(storeKey + "_Job", "CrawlerGroup").storeDurably().usingJobData(dataMap).build();

                // 3. Build unique Cron Trigger using configuration string
                Trigger trigger = TriggerBuilder.newTrigger().forJob(jobDetail)
                        .withIdentity(storeKey + "_Trigger", "CrawlerGroup")
                        .withSchedule(CronScheduleBuilder.cronSchedule(jobConfig.cron())).build();

                // 4. Schedule the job dynamically
                scheduler.scheduleJob(jobDetail, trigger);
                log.info("Successfully scheduled crawler job '{}' with cron '{}'", jobConfig.name(), jobConfig.cron());

            } catch (SchedulerException e) {
                log.error("Failed to programmatically schedule Quartz job for store: {}", storeKey, e);
            }
        }
    }
}
