package file.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

import file.infrastructure.enumerate.FileStatus;

@Entity
@Table(name = "files")
public class FileEntity {
    @Id
    private UUID id;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "object_key", nullable = false, updatable = false)
    private String objectKey;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    private String extension;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long size;

    private String checksum;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FileStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected FileEntity() {}
    public FileEntity(
        UUID id,
        UUID ownerId,
        String objectKey,
        String originalName,
        String extension,
        String contentType,
        long size,
        String checksum,
        FileStatus status,
        Instant now
    ) {
        this.id = id;
        this.ownerId = ownerId;
        this.objectKey = objectKey;
        this.originalName = originalName;
        this.extension = extension;
        this.contentType = contentType;
        this.size = size;
        this.checksum = checksum;
        this.status = status;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getExtension() {
        return extension;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSize() {
        return size;
    }

    public String getChecksum() {
        return checksum;
    }

    public FileStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setStatus(FileStatus status, Instant now) {
        this.status = status;
        this.updatedAt = now;
    }

    public void setChecksum(String checksum, Instant now) {
        this.checksum = checksum;
        this.updatedAt = now;
    }

    public void markDeleted(Instant now) {
        this.deletedAt = now;
        this.updatedAt = now;
    }
}
