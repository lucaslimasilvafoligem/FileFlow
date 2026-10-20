package processing.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import processing.infrastructure.enumerate.ArtifactType;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "artifacts")
public class ArtifactEntity {
    @Id
    private UUID id;

    @Column(name = "file_id", nullable = false, updatable = false)
    private UUID fileId;

    @Column(name = "job_id", nullable = false, updatable = false)
    private UUID jobId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private ArtifactType type;

    @Column(nullable = false, updatable = false)
    private String name;

    @Column(name = "object_key", nullable = false, updatable = false)
    private String objectKey;

    @Column(name = "content_type", updatable = false)
    private String contentType;

    @Column(nullable = false, updatable = false)
    private long size;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ArtifactEntity() {}

    public ArtifactEntity(
        UUID id,
        UUID fileId,
        UUID jobId,
        ArtifactType type,
        String name,
        String objectKey,
        String contentType,
        long size,
        Instant createdAt
    ) {
        this.id = id;
        this.fileId = fileId;
        this.jobId = jobId;
        this.type = type;
        this.name = name;
        this.objectKey = objectKey;
        this.contentType = contentType;
        this.size = size;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getFileId() {
        return fileId;
    }

    public UUID getJobId() {
        return jobId;
    }

    public ArtifactType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSize() {
        return size;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
