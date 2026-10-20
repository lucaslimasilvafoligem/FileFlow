package file.infrastructure.dto.response;

import java.time.Instant;
import java.util.UUID;

import file.infrastructure.enumerate.FileStatus;
import file.infrastructure.record.FileRecord;

/** Contrato público do arquivo: não expõe objectKey nem ownerId. */
public record FileResponse(
        UUID id,
        String originalName,
        String extension,
        String contentType,
        long size,
        String checksum,
        FileStatus status,
        Instant createdAt,
        Instant updatedAt
    ) {

    public static FileResponse from(FileRecord f) {
        return new FileResponse(
            f.id(),
            f.originalName(),
            f.extension(),
            f.contentType(),
            f.size(),
            f.checksum(),
            f.status(),
            f.createdAt(),
            f.updatedAt()
        );
    }
}
