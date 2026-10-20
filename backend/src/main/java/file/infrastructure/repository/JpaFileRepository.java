package file.infrastructure.repository;

import file.domain.FileSpecifications;
import file.infrastructure.entity.FileEntity;
import file.infrastructure.enumerate.FileSortField;
import file.infrastructure.enumerate.SortDirection;
import file.infrastructure.record.FilePage;
import file.infrastructure.record.FileRecord;
import file.infrastructure.record.FileSearchCriteria;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class JpaFileRepository implements FileRepository {
    private final FileJpaRepository jpa;

    JpaFileRepository(FileJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public FileRecord add(FileRecord r) {
        FileEntity entity = new FileEntity(
            r.id(),
            r.ownerId(),
            r.objectKey(),
            r.originalName(),
            r.extension(),
            r.contentType(),
            r.size(),
            r.checksum(),
            r.status(),
            r.createdAt()
        );
        return toRecord(jpa.save(entity));
    }

    @Override
    public Optional<FileRecord> findById(UUID id) {
        return jpa.findById(id).map(JpaFileRepository::toRecord);
    }

    @Override
    public Optional<FileRecord> findActiveById(UUID id) {
        return jpa.findByIdAndDeletedAtIsNull(id).map(JpaFileRepository::toRecord);
    }

    @Override
    @Transactional
    public void markDeleted(UUID id, Instant now) {
        jpa.findById(id).ifPresent(entity -> {
            if (entity.getDeletedAt() == null) {
                entity.markDeleted(now);
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public FilePage search(FileSearchCriteria c) {
        Sort.Direction direction = c.direction() == SortDirection.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, property(c.sort())).and(Sort.by("id"));
        Page<FileEntity> page = jpa.findAll(FileSpecifications.from(c), PageRequest.of(c.page(), c.size(), sort));
        return new FilePage(page.getContent().stream().map(JpaFileRepository::toRecord).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    private static String property(FileSortField field) {
        return switch (field) {
            case CREATED_AT -> "createdAt";
            case SIZE -> "size";
            case NAME -> "originalName";
        };
    }

    private static FileRecord toRecord(FileEntity e) {
        return new FileRecord(
            e.getId(),
            e.getOwnerId(),
            e.getObjectKey(),
            e.getOriginalName(),
            e.getExtension(),
            e.getContentType(),
            e.getSize(),
            e.getChecksum(),
            e.getStatus(),
            e.getCreatedAt(),
            e.getUpdatedAt(),
            e.getDeletedAt()
        );
    }
}
