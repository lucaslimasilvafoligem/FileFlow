package processing.infrastructure.enumerate;

public enum JobStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    DEAD_LETTER,
    CANCELLED;

    public boolean canTransitionTo(JobStatus next) {
        return switch (this) {
            case PENDING -> next == PROCESSING || next == CANCELLED || next == DEAD_LETTER;
            case PROCESSING -> next == COMPLETED || next == PENDING || next == FAILED || next == DEAD_LETTER;
            case COMPLETED, FAILED, DEAD_LETTER, CANCELLED -> false;
        };
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == DEAD_LETTER || this == CANCELLED;
    }
}
