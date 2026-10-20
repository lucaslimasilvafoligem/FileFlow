package file.infrastructure.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import file.infrastructure.entity.FileEntity;

public interface FileJpaRepository extends JpaRepository<FileEntity, UUID>, JpaSpecificationExecutor<FileEntity> {
    Optional<FileEntity> findByIdAndDeletedAtIsNull(UUID id);
}
