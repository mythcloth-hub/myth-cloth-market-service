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
     * @param listing
     *            the crawler message to publish
     */
    public void publishCrawlerMessage(StoreListing listing) {
        try {
            if (Objects.isNull(listing.lineUp())) {
                log.warn(
                        "Skipping message publishing for store listing due to lineup could not be detected: {} - [{}] => {}, url: {}",
                        listing.store(), listing.productName(), listing.originalProductName(), listing.productUrl());
                return;
            }

            rabbitTemplate.convertAndSend(RabbitMQConfig.ExchangeNames.CRAWLER_EXCHANGE, "crawler.job", listing);
            log.info("Message published to crawler exchange: {} - {} ${} [{}] => {}", listing.store(), listing.lineUp(),
                    listing.price(), listing.productName(), listing.originalProductName());
        } catch (Exception e) {
            log.error("Error publishing crawler message: {}", listing, e);
            throw e;
        }
    }
}
