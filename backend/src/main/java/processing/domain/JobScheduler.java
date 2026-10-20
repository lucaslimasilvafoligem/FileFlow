package processing.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import file.infrastructure.record.FileStored;
import messaging.infrastructure.contract.OutboxRepository;
import processing.infrastructure.enumerate.JobType;
import processing.infrastructure.repository.JobRepository;

/**
 * Cria os jobs de um arquivo recém-gravado e o evento de outbox de cada um. Executa na transação de quem
 * publicou o evento. A idempotency key ({@code fileId:JOB_TYPE}) impede jobs duplicados.
 */
@Component
public class JobScheduler {
    private final JobPlanner planner;
    private final JobRepository jobs;
    private final OutboxRepository outbox;
    private final Clock clock;

    public JobScheduler(JobPlanner planner, JobRepository jobs, OutboxRepository outbox, Clock clock) {
        this.planner = planner;
        this.jobs = jobs;
        this.outbox = outbox;
        this.clock = clock;
    }

    @EventListener
    public void onFileStored(FileStored event) {
        Instant now = clock.instant();
        for (JobType type : planner.plan(event.contentType())) {
            UUID jobId = UUID.randomUUID();
            jobs.add(jobId, event.fileId(), type, event.fileId() + ":" + type.name(), now);
            outbox.add(event.fileId(), OutboxRepository.JOB_DISPATCH, Map.of(
                    "jobId", jobId.toString(),
                    "fileId", event.fileId().toString(),
                    "jobType", type.name()), now);
        }
    }
}
