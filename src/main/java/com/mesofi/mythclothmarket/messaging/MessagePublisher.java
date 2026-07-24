package com.mesofi.mythclothmarket.messaging;

import java.util.Objects;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.mesofi.mythclothmarket.crawler.model.StoreListing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publishes a crawler message object to the crawler queue.
     *
     * @param storeListing
     *            the crawler message to publish
     */
    public void publishCrawlerMessage(StoreListing storeListing) {
        try {
            if (Objects.isNull(storeListing.lineUp())) {
                log.warn("Skipping message publishing for store listing due to null lineup: {} - [{}], url: {}",
                        storeListing.store(), storeListing.productName(), storeListing.productUrl());
                return;
            }

            rabbitTemplate.convertAndSend(RabbitMQConfig.ExchangeNames.CRAWLER_EXCHANGE, "crawler.job", storeListing);
            log.info("Message published to crawler exchange: {} - {} [{}]", storeListing.store(), storeListing.lineUp(),
                    storeListing.productName());
        } catch (Exception e) {
            log.error("Error publishing crawler message: {}", storeListing, e);
            throw e;
        }
    }
}
