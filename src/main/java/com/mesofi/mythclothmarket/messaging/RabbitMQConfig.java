package com.mesofi.mythclothmarket.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static class QueueNames {
        public static final String CRAWLER_QUEUE = "crawler.queue";

        private QueueNames() {
        }
    }

    public static class ExchangeNames {
        public static final String CRAWLER_EXCHANGE = "crawler.exchange";

        private ExchangeNames() {
        }
    }

    public static class RoutingKeys {
        public static final String CRAWLER_ROUTING_KEY = "crawler.#";

        private RoutingKeys() {
        }
    }

    // --- Exchanges ---
    @Bean
    public DirectExchange crawlerExchange() {
        return new DirectExchange(ExchangeNames.CRAWLER_EXCHANGE, true, false);
    }

    // --- Queues ---
    @Bean
    public Queue crawlerQueue() {
        return QueueBuilder.durable(QueueNames.CRAWLER_QUEUE).build();
    }

    // --- Bindings ---
    @Bean
    public Binding crawlerBinding(Queue crawlerQueue, DirectExchange crawlerExchange) {
        return BindingBuilder.bind(crawlerQueue).to(crawlerExchange).with(RoutingKeys.CRAWLER_ROUTING_KEY);
    }

    // --- Message Converter ---
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
