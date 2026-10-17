package com.hkuer.hkuer.messaging;

public record InteractionEventMessage(
        String eventId,
        String eventType,
        String aggregateType,
        String aggregateId,
        String actorId,
        String occurredAt,
        int schemaVersion) {
}
