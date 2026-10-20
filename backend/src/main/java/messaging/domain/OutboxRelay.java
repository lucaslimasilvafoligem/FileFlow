package messaging.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import messaging.config.RabbitTopology;
import messaging.infrastructure.entity.OutboxEventEntity;
import messaging.infrastructure.record.MessagingProperties;
import messaging.infrastructure.repository.OutboxEventJpaRepository;
import processing.infrastructure.enumerate.JobType;

/**
 * Publica os eventos do outbox no RabbitMQ. Cada lote roda em uma transação: os eventos são travados
 * (SKIP LOCKED), publicados com confirmação do broker e só então marcados como publicados. Se algo falha,
 * o rollback devolve os eventos e eles são reenviados: a entrega é at-least-once, por isso o consumidor
 * precisa ser idempotente.
 */
@Component
class OutboxRelay {
    private static final Logger LOG = LoggerFactory.getLogger(OutboxRelay.class);
    private static final long CONFIRM_TIMEOUT_MS = 5000;
    private final OutboxEventJpaRepository events;
    private final RabbitTemplate rabbit;
    private final TransactionTemplate transaction;
    private final Clock clock;
    private final int batchSize;

    OutboxRelay(
        OutboxEventJpaRepository events,
        RabbitTemplate rabbit,
        PlatformTransactionManager txManager,
        Clock clock, MessagingProperties properties
    ) {
        this.events = events;
        this.rabbit = rabbit;
        this.transaction = new TransactionTemplate(txManager);
        this.clock = clock;
        this.batchSize = properties.outbox().batchSize();
    }

    @Scheduled(fixedDelayString = "${messaging.outbox.poll-interval-ms:1000}")
    public void poll() {
        try {
            int relayed;
            do {
                Integer result = transaction.execute(status -> relayBatch());
                relayed = result == null ? 0 : result;
            } while (relayed == batchSize);
        } catch (RuntimeException e) {
            LOG.warn("outbox.relay failed; retrying on next poll: {}", e.toString());
        }
    }

    private int relayBatch() {
        List<OutboxEventEntity> batch = events.lockPending(PageRequest.ofSize(batchSize));
        if (batch.isEmpty()) {
            return 0;
        }
        rabbit.invoke(operations -> {
            for (OutboxEventEntity event : batch) {
                operations.convertAndSend(
                    RabbitTopology.JOBS_EXCHANGE,
                    routingKey(event),
                    event.getPayload(),
                    message -> {
                        message.getMessageProperties().setMessageId(event.getId().toString());
                        return message;
                    }
                );
            }
            operations.waitForConfirmsOrDie(CONFIRM_TIMEOUT_MS);
            return null;
        });
        Instant now = clock.instant();
        batch.forEach(event -> event.markPublished(now));
        LOG.debug("outbox.relay published={}", batch.size());
        return batch.size();
    }

    private static String routingKey(OutboxEventEntity event) {
        String jobType = String.valueOf(event.getPayload().get("jobType"));
        return JobType.valueOf(jobType).family().routingKey();
    }
}
