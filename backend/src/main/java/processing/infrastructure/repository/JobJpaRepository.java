package processing.infrastructure.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import processing.infrastructure.entity.JobEntity;

public interface JobJpaRepository extends JpaRepository<JobEntity, UUID> {
    List<JobEntity> findByFileId(UUID fileId);
    Optional<JobEntity> findByIdempotencyKey(String idempotencyKey);
}
