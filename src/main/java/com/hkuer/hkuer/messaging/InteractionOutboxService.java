package com.hkuer.hkuer.messaging;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hkuer.hkuer.config.RabbitMqConfig;
import com.hkuer.hkuer.entity.PO.OutboxEvent;
import com.hkuer.hkuer.mapper.userface.OutboxEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class InteractionOutboxService {

    private static final String STATUS_NEW = "NEW";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_FAILED = "FAILED";
    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRIES = 10;

    private final OutboxEventMapper outboxEventMapper;
    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;

    @org.springframework.beans.factory.annotation.Value("${mesh.messaging-enabled:true}")
    private boolean messagingEnabled = true;
    @org.springframework.beans.factory.annotation.Value("${mesh.local-outbox:false}")
    private boolean localOutbox;
    @org.springframework.beans.factory.annotation.Autowired
    private InteractionCacheService localCache;

    /**
     * 必须由业务事务内部调用，业务数据和 Outbox 才会一起提交或一起回滚。
     */
    public void append(String eventType, String aggregateType, String aggregateId, String actorId) {
        String eventId = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime now = LocalDateTime.now();
        InteractionEventMessage message = new InteractionEventMessage(
                eventId, eventType, aggregateType, aggregateId, actorId, now.toString(), 1);

        OutboxEvent event = new OutboxEvent();
        event.setEventId(eventId);
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setEventType(eventType);
        event.setRoutingKey(InteractionEventTypes.routingKey(eventType));
        event.setPayload(toJson(message));
        event.setStatus(STATUS_NEW);
        event.setRetryCount(0);
        event.setNextRetryAt(now);
        event.setCreatedAt(now);
        outboxEventMapper.insert(event);
    }

    @Scheduled(fixedDelay = 1000, initialDelay = 5000)
    public void publishPendingEvents() {
        if (!messagingEnabled && !localOutbox) return;
        List<OutboxEvent> events = outboxEventMapper.selectList(
                new LambdaQueryWrapper<OutboxEvent>()
                        .eq(OutboxEvent::getStatus, STATUS_NEW)
                        .le(OutboxEvent::getNextRetryAt, LocalDateTime.now())
                        .orderByAsc(OutboxEvent::getCreatedAt)
                        .last("LIMIT " + BATCH_SIZE));

        for (OutboxEvent event : events) {
            try {
                if (localOutbox) localCache.evict(objectMapper.readValue(event.getPayload(), InteractionEventMessage.class));
                else publish(event);
                markPublished(event);
            } catch (Exception e) {
                markRetry(event, e);
                break; // Broker 故障时本轮不继续制造重复失败日志
            }
        }
    }

    @Scheduled(cron = "0 30 3 * * ?")
    public void cleanPublishedEvents() {
        int deleted = outboxEventMapper.delete(new QueryWrapper<OutboxEvent>()
                .eq("status", STATUS_PUBLISHED)
                .lt("published_at", LocalDateTime.now().minusDays(7)));
        if (deleted > 0) {
            log.info("已清理过期 Outbox 事件: {}", deleted);
        }
    }

    private void publish(OutboxEvent event) throws Exception {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        properties.setMessageId(event.getEventId());
        Message message = new Message(event.getPayload().getBytes(StandardCharsets.UTF_8), properties);

        CorrelationData correlationData = new CorrelationData(event.getEventId());
        rabbitTemplate.send(RabbitMqConfig.EVENT_EXCHANGE, event.getRoutingKey(), message, correlationData);
        CorrelationData.Confirm confirm = correlationData.getFuture().get(5, TimeUnit.SECONDS);
        if (!confirm.isAck()) {
            throw new IllegalStateException("RabbitMQ 未确认消息: " + confirm.getReason());
        }
        if (correlationData.getReturned() != null) {
            throw new IllegalStateException("RabbitMQ 消息没有匹配到队列");
        }
    }

    private void markPublished(OutboxEvent event) {
        event.setStatus(STATUS_PUBLISHED);
        event.setPublishedAt(LocalDateTime.now());
        event.setLastError(null);
        outboxEventMapper.updateById(event);
    }

    private void markRetry(OutboxEvent event, Exception exception) {
        int retryCount = event.getRetryCount() + 1;
        event.setRetryCount(retryCount);
        event.setStatus(retryCount >= MAX_RETRIES ? STATUS_FAILED : STATUS_NEW);
        long delaySeconds = Math.min(300, 5L << Math.min(retryCount - 1, 6));
        event.setNextRetryAt(LocalDateTime.now().plusSeconds(delaySeconds));
        event.setLastError(truncate(exception.getMessage()));
        outboxEventMapper.updateById(event);
        log.warn("互动事件发布失败，eventId: {}, retryCount: {}", event.getEventId(), retryCount, exception);
    }

    private String toJson(InteractionEventMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("互动事件序列化失败", e);
        }
    }

    private String truncate(String message) {
        if (message == null) {
            return "unknown error";
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
