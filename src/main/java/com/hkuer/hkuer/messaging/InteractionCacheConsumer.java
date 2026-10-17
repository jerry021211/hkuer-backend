package com.hkuer.hkuer.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hkuer.hkuer.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class InteractionCacheConsumer {

    private final ObjectMapper objectMapper;
    private final InteractionCacheService cacheService;

    @RabbitListener(queues = RabbitMqConfig.CACHE_QUEUE)
    public void invalidateCache(Message message) throws IOException {
        InteractionEventMessage event = objectMapper.readValue(message.getBody(), InteractionEventMessage.class);
        cacheService.evict(event);
        log.debug("互动缓存已失效，eventId: {}, eventType: {}", event.eventId(), event.eventType());
    }
}
