package file.infrastructure.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import file.infrastructure.record.FilePage;
import file.infrastructure.record.FileRecord;
import file.infrastructure.record.FileSearchCriteria;

/** Porta de persistência de arquivos. */
public interface FileRepository {
    FileRecord add(FileRecord file);

    /** Inclui arquivos excluídos (soft delete). */
    Optional<FileRecord> findById(UUID id);

    Optional<FileRecord> findActiveById(UUID id);

    void markDeleted(UUID id, Instant now);

    FilePage search(FileSearchCriteria criteria);
}
