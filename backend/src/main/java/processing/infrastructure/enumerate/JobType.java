package processing.infrastructure.enumerate;

public enum JobType {
    FILE_METADATA(ProcessingFamily.FILE),
    IMAGE_METADATA(ProcessingFamily.IMAGE),
    IMAGE_THUMBNAIL(ProcessingFamily.IMAGE),
    AUDIO_METADATA(ProcessingFamily.AUDIO),
    AUDIO_TRANSCRIPTION(ProcessingFamily.AUDIO),
    VIDEO_METADATA(ProcessingFamily.VIDEO),
    VIDEO_THUMBNAIL(ProcessingFamily.VIDEO),
    VIDEO_AUDIO_EXTRACTION(ProcessingFamily.VIDEO),
    DOCUMENT_METADATA(ProcessingFamily.DOCUMENT),
    DOCUMENT_TEXT_EXTRACTION(ProcessingFamily.DOCUMENT);

    private final ProcessingFamily family;

    JobType(ProcessingFamily family) {
        this.family = family;
    }

    public ProcessingFamily family() {
        return family;
    }
}
