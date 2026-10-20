package messaging.infrastructure.repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import messaging.infrastructure.entity.OutboxEventEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, UUID> {
    List<OutboxEventEntity> findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();

    /**
     * Eventos pendentes em ordem, com lock de escrita que PULA linhas já travadas por outra instância
     * (FOR UPDATE SKIP LOCKED): dois relays nunca disputam o mesmo evento.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select e from OutboxEventEntity e where e.publishedAt is null order by e.createdAt asc, e.id asc")
    List<OutboxEventEntity> lockPending(Pageable pageable);
}
