package processing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

import processing.infrastructure.enumerate.JobStatus;

class JobStatusTest {
    @Test
    void pendingCanStartCancelOrDie() {
        assertThat(JobStatus.PENDING.canTransitionTo(JobStatus.PROCESSING)).isTrue();
        assertThat(JobStatus.PENDING.canTransitionTo(JobStatus.CANCELLED)).isTrue();
        assertThat(JobStatus.PENDING.canTransitionTo(JobStatus.DEAD_LETTER)).isTrue();
        assertThat(JobStatus.PENDING.canTransitionTo(JobStatus.COMPLETED)).isFalse();
    }

    @Test
    void processingCanFinishRetryOrFail() {
        assertThat(JobStatus.PROCESSING.canTransitionTo(JobStatus.COMPLETED)).isTrue();
        assertThat(JobStatus.PROCESSING.canTransitionTo(JobStatus.PENDING)).isTrue();
        assertThat(JobStatus.PROCESSING.canTransitionTo(JobStatus.FAILED)).isTrue();
        assertThat(JobStatus.PROCESSING.canTransitionTo(JobStatus.DEAD_LETTER)).isTrue();
        assertThat(JobStatus.PROCESSING.canTransitionTo(JobStatus.CANCELLED)).isFalse();
    }

    @Test
    void terminalStatesAcceptNoTransition() {
        Arrays.stream(JobStatus.values()).filter(JobStatus::isTerminal).forEach(terminal ->
                Arrays.stream(JobStatus.values()).forEach(next ->
                        assertThat(terminal.canTransitionTo(next))
                                .as("%s -> %s", terminal, next)
                                .isFalse()));
    }
}
