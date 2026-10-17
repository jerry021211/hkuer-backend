package com.hkuer.hkuer.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String EVENT_EXCHANGE = "hkuer.interaction.events";
    public static final String CACHE_QUEUE = "hkuer.interaction.cache.invalidate";
    public static final String DEAD_EXCHANGE = "hkuer.interaction.dlx";
    public static final String DEAD_QUEUE = "hkuer.interaction.cache.invalidate.dlq";
    public static final String DEAD_ROUTING_KEY = "interaction.cache.dead";

    @Bean
    public TopicExchange interactionEventExchange() {
        return new TopicExchange(EVENT_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange interactionDeadExchange() {
        return new DirectExchange(DEAD_EXCHANGE, true, false);
    }

    @Bean
    public Queue interactionCacheQueue() {
        return QueueBuilder.durable(CACHE_QUEUE)
                .deadLetterExchange(DEAD_EXCHANGE)
                .deadLetterRoutingKey(DEAD_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue interactionDeadQueue() {
        return QueueBuilder.durable(DEAD_QUEUE).build();
    }

    @Bean
    public Binding interactionCacheBinding(Queue interactionCacheQueue, TopicExchange interactionEventExchange) {
        return BindingBuilder.bind(interactionCacheQueue).to(interactionEventExchange).with("interaction.#");
    }

    @Bean
    public Binding interactionDeadBinding(Queue interactionDeadQueue, DirectExchange interactionDeadExchange) {
        return BindingBuilder.bind(interactionDeadQueue).to(interactionDeadExchange).with(DEAD_ROUTING_KEY);
    }
}
