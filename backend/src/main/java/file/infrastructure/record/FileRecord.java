package file.infrastructure.record;

import java.time.Instant;
import java.util.UUID;

import file.infrastructure.enumerate.FileStatus;

/** Visão de domínio de um arquivo armazenado. */
public record FileRecord(
        UUID id,
        UUID ownerId,
        String objectKey,
        String originalName,
        String extension,
        String contentType,
        long size,
        String checksum,
        FileStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt) {

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
