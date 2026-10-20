package file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import bootstrap.FileManagementApplication;
import file.infrastructure.entity.FileEntity;
import file.infrastructure.enumerate.FileStatus;
import file.infrastructure.repository.FileJpaRepository;
import messaging.infrastructure.entity.OutboxEventEntity;
import messaging.infrastructure.repository.OutboxEventJpaRepository;
import processing.infrastructure.entity.ArtifactEntity;
import processing.infrastructure.entity.JobEntity;
import processing.infrastructure.enumerate.ArtifactType;
import processing.infrastructure.enumerate.JobType;
import processing.infrastructure.repository.ArtifactJpaRepository;
import processing.infrastructure.repository.JobJpaRepository;

@SpringBootTest(
    classes = FileManagementApplication.class,
    properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "storage.initialize-bucket=false"
    }
)
@Testcontainers
@Transactional
class PersistenceIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private FileJpaRepository files;

    @Autowired
    private JobJpaRepository jobs;

    @Autowired
    private ArtifactJpaRepository artifacts;

    @Autowired
    private OutboxEventJpaRepository outbox;

    @Test
    void persistsFileJobAndArtifact() {
        FileEntity file = files.saveAndFlush(
            newFile("objects/" + UUID.randomUUID() + ".jpg")
        );

        JobEntity job = jobs.saveAndFlush(
            new JobEntity(
                UUID.randomUUID(),
                file.getId(),
                JobType.IMAGE_THUMBNAIL,
                "key-" + UUID.randomUUID(),
                Map.of("size", "small"),
                Instant.now()
            )
        );

        artifacts.saveAndFlush(
            new ArtifactEntity(
                UUID.randomUUID(),
                file.getId(),
                job.getId(),
                ArtifactType.THUMBNAIL,
                "thumbnail-small.jpg",
                "derived/" + UUID.randomUUID(),
                "image/jpeg",
                10,
                Instant.now()
            )
        );

        assertThat(jobs.findByFileId(file.getId()))
            .singleElement()
            .satisfies(j ->
                assertThat(j.getPayload()).containsEntry("size", "small")
            );

        assertThat(artifacts.findByFileId(file.getId())).hasSize(1);
    }

    @Test
    void duplicateIdempotencyKeyIsRejected() {
        FileEntity file = files.saveAndFlush(
            newFile("objects/" + UUID.randomUUID() + ".txt")
        );

        String key = "same-key-" + UUID.randomUUID();

        jobs.saveAndFlush(newJob(file.getId(), key));

        assertThatThrownBy(
            () -> jobs.saveAndFlush(newJob(file.getId(), key))
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duplicateObjectKeyIsRejected() {
        String key = "objects/" + UUID.randomUUID() + ".txt";

        files.saveAndFlush(newFile(key));

        assertThatThrownBy(
            () -> files.saveAndFlush(newFile(key))
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void softDeletedFileIsNotFoundByActiveQuery() {
        FileEntity file = files.saveAndFlush(
            newFile("objects/" + UUID.randomUUID() + ".txt")
        );

        UUID fileId = file.getId();

        file.markDeleted(Instant.now());
        files.saveAndFlush(file);

        assertThat(files.findByIdAndDeletedAtIsNull(fileId)).isEmpty();

        assertThat(files.findById(fileId))
            .isPresent()
            .get()
            .extracting(FileEntity::getDeletedAt)
            .isNotNull();
    }

    @Test
    void outboxReturnsOnlyUnpublishedEventsInOrder() {
        Instant base = Instant.now().truncatedTo(ChronoUnit.MILLIS);

        OutboxEventEntity first = outbox.saveAndFlush(newEvent(base));
        OutboxEventEntity published =
            outbox.saveAndFlush(newEvent(base.plusSeconds(1)));
        OutboxEventEntity second =
            outbox.saveAndFlush(newEvent(base.plusSeconds(2)));

        UUID firstId = first.getId();
        UUID publishedId = published.getId();
        UUID secondId = second.getId();

        published.markPublished(Instant.now());
        outbox.saveAndFlush(published);

        List<OutboxEventEntity> pending =
            outbox.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();

        Set<UUID> pendingIds = pending.stream()
            .map(OutboxEventEntity::getId)
            .collect(Collectors.toSet());

        assertThat(pendingIds)
            .contains(firstId, secondId)
            .doesNotContain(publishedId);

        assertThat(pending.stream().map(OutboxEventEntity::getCreatedAt).toList())
            .isSorted();
    }

    @Test
    void lockPendingSkipsPublishedEvents() {
        OutboxEventEntity pending = outbox.save(newEvent(Instant.now()));
        OutboxEventEntity done = outbox.save(newEvent(Instant.now()));
        done.markPublished(Instant.now());
        outbox.flush();

        List<OutboxEventEntity> locked = outbox.lockPending(PageRequest.ofSize(50));

        assertThat(locked).extracting(OutboxEventEntity::getId)
                .contains(pending.getId())
                .doesNotContain(done.getId());
    }

    private static FileEntity newFile(String objectKey) {
        return new FileEntity(
            UUID.randomUUID(),
            null,
            objectKey,
            "foto.jpg",
            "jpg",
            "image/jpeg",
            123,
            null,
            FileStatus.UPLOADED,
            Instant.now()
        );
    }

    private static JobEntity newJob(UUID fileId, String idempotencyKey) {
        return new JobEntity(
            UUID.randomUUID(),
            fileId,
            JobType.FILE_METADATA,
            idempotencyKey,
            null,
            Instant.now()
        );
    }

    private static OutboxEventEntity newEvent(Instant createdAt) {
        return new OutboxEventEntity(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "file.stored",
            Map.of("k", "v"),
            createdAt
        );
    }
}
