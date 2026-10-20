package processing.infrastructure.repository;

import java.time.Instant;
import java.util.UUID;

import processing.infrastructure.enumerate.JobType;

/** Porta de persistência de jobs. */
public interface JobRepository {

    /** Cria o job em PENDING. A idempotency key é única: duplicatas violam a constraint. */
    void add(UUID id, UUID fileId, JobType type, String idempotencyKey, Instant now);
}
