package processing.infrastructure.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import processing.infrastructure.entity.ArtifactEntity;

public interface ArtifactJpaRepository extends JpaRepository<ArtifactEntity, UUID> {
    List<ArtifactEntity> findByFileId(UUID fileId);
}
