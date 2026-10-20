package messaging.infrastructure.contract;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Porta do outbox: eventos gravados na mesma transação do estado que os originou. */
public interface OutboxRepository {
    String JOB_DISPATCH = "job.dispatch";

    void add(UUID aggregateId, String eventType, Map<String, Object> payload, Instant now);
}
