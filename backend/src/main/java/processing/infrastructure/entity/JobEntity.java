package processing.infrastructure.entity;

import processing.infrastructure.enumerate.JobStatus;
import processing.infrastructure.enumerate.JobType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "jobs")
public class JobEntity {
    @Id
    private UUID id;

    @Column(name = "file_id", nullable = false, updatable = false)
    private UUID fileId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private JobType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "idempotency_key", nullable = false, updatable = false)
    private String idempotencyKey;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> payload;

    @Column(name = "available_at", nullable = false)
    private Instant availableAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    private String error;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Optimistic locking: workers e watchdog podem atualizar o mesmo job concorrentemente. */
    @Version
    private long version;

    protected JobEntity() {}

    public JobEntity(
        UUID id,
        UUID fileId,
        JobType type,
        String idempotencyKey,
        Map<String, Object> payload, 
        Instant now
    ) {
        this.id = id;
        this.fileId = fileId;
        this.type = type;
        this.status = JobStatus.PENDING;
        this.attempts = 0;
        this.idempotencyKey = idempotencyKey;
        this.payload = payload;
        this.availableAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getFileId() {
        return fileId;
    }

    public JobType getType() {
        return type;
    }

    public JobStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public Instant getAvailableAt() {
        return availableAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getError() {
        return error;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }

    public void setStatus(JobStatus status, Instant now) {
        this.status = status;
        this.updatedAt = now;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public void setAvailableAt(Instant availableAt) {
        this.availableAt = availableAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public void setError(String error) {
        this.error = error;
    }
}
