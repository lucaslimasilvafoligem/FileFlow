package processing.infrastructure.repository;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Repository;

import processing.infrastructure.entity.JobEntity;
import processing.infrastructure.enumerate.JobType;

/**
 * Adaptador de persistência que conecta a porta JobRepository
 * à implementação Spring Data JPA.
 */
@Repository
public class JobPersistenceAdapter implements JobRepository {
    private final JobJpaRepository jobJpaRepository;

    public JobPersistenceAdapter(JobJpaRepository jobJpaRepository) {
        this.jobJpaRepository = jobJpaRepository;
    }

    /**
     * Persiste um novo job com status PENDING.
     *
     * A chave de idempotência é armazenada para impedir a criação
     * de jobs duplicados, conforme a constraint definida no banco.
     */
    @Override
    public void add(
            UUID id,
            UUID fileId,
            JobType type,
            String idempotencyKey,
            Instant now) {

        JobEntity job = new JobEntity(
                id,
                fileId,
                type,
                idempotencyKey,
                Map.of(),
                now
        );

        jobJpaRepository.save(job);
    }
}
