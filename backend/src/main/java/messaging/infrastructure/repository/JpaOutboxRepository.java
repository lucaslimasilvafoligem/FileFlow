package messaging.infrastructure.repository;

import messaging.infrastructure.contract.OutboxRepository;
import messaging.infrastructure.entity.OutboxEventEntity;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class JpaOutboxRepository implements OutboxRepository {
    private final OutboxEventJpaRepository jpa;

    JpaOutboxRepository(OutboxEventJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void add(UUID aggregateId, String eventType, Map<String, Object> payload, Instant now) {
        jpa.save(new OutboxEventEntity(UUID.randomUUID(), aggregateId, eventType, payload, now));
    }
}
