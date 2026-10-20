package processing;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import file.infrastructure.record.FileStored;
import messaging.infrastructure.contract.OutboxRepository;
import processing.config.ProcessingProperties;
import processing.domain.JobPlanner;
import processing.domain.JobScheduler;
import processing.infrastructure.enumerate.JobType;
import processing.infrastructure.repository.JobRepository;

@ExtendWith(MockitoExtension.class)
class JobSchedulerTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private JobRepository jobs;
    @Mock
    private OutboxRepository outbox;

    private JobScheduler scheduler(JobType... enabled) {
        return new JobScheduler(new JobPlanner(new ProcessingProperties(List.of(enabled))), jobs, outbox,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsOneJobAndOneOutboxEventPerPlannedType() {
        UUID fileId = UUID.randomUUID();

        scheduler(JobType.FILE_METADATA, JobType.IMAGE_THUMBNAIL)
                .onFileStored(new FileStored(fileId, "image/png", 10));

        verify(jobs).add(any(), eq(fileId), eq(JobType.FILE_METADATA), eq(fileId + ":FILE_METADATA"), eq(NOW));
        verify(jobs).add(any(), eq(fileId), eq(JobType.IMAGE_THUMBNAIL), eq(fileId + ":IMAGE_THUMBNAIL"), eq(NOW));
        verify(outbox).add(eq(fileId), eq(OutboxRepository.JOB_DISPATCH),
                argThat(payload -> "IMAGE_THUMBNAIL".equals(payload.get("jobType"))), eq(NOW));
        verify(outbox).add(eq(fileId), eq(OutboxRepository.JOB_DISPATCH),
                argThat(payload -> "FILE_METADATA".equals(payload.get("jobType"))), eq(NOW));
    }

    @Test
    void createsNothingWhenNoJobTypeIsEnabled() {
        scheduler().onFileStored(new FileStored(UUID.randomUUID(), "image/png", 10));

        verifyNoInteractions(jobs, outbox);
    }
}
