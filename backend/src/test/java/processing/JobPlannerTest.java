package processing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

import processing.config.ProcessingProperties;
import processing.domain.JobPlanner;
import processing.infrastructure.enumerate.JobType;

class JobPlannerTest {

    private static JobPlanner planner(JobType... enabled) {
        return new JobPlanner(new ProcessingProperties(List.of(enabled)));
    }

    @Test
    void onlyFileMetadataWhenNothingElseIsEnabled() {
        assertThat(planner(JobType.FILE_METADATA).plan("image/png")).containsExactly(JobType.FILE_METADATA);
    }

    @Test
    void imageGetsThumbnailWhenEnabled() {
        assertThat(planner(JobType.FILE_METADATA, JobType.IMAGE_THUMBNAIL).plan("image/jpeg"))
                .containsExactly(JobType.FILE_METADATA, JobType.IMAGE_THUMBNAIL);
    }

    @Test
    void matchingIsCaseInsensitive() {
        assertThat(planner(JobType.FILE_METADATA, JobType.IMAGE_THUMBNAIL).plan("IMAGE/PNG"))
                .contains(JobType.IMAGE_THUMBNAIL);
    }

    @Test
    void audioGetsTranscription() {
        assertThat(planner(JobType.FILE_METADATA, JobType.AUDIO_TRANSCRIPTION).plan("audio/mpeg"))
                .containsExactly(JobType.FILE_METADATA, JobType.AUDIO_TRANSCRIPTION);
    }

    @Test
    void videoGetsMetadataThumbnailAndAudioExtraction() {
        JobPlanner all = planner(JobType.values());

        assertThat(all.plan("video/mp4")).containsExactly(JobType.FILE_METADATA, JobType.VIDEO_METADATA,
                JobType.VIDEO_THUMBNAIL, JobType.VIDEO_AUDIO_EXTRACTION);
    }

    @Test
    void documentsGetTextExtraction() {
        JobPlanner all = planner(JobType.values());

        assertThat(all.plan("application/pdf")).contains(JobType.DOCUMENT_TEXT_EXTRACTION);
        assertThat(all.plan("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .contains(JobType.DOCUMENT_TEXT_EXTRACTION);
    }

    @Test
    void unknownTypeOnlyGetsFileMetadata() {
        assertThat(planner(JobType.values()).plan("application/octet-stream"))
                .containsExactly(JobType.FILE_METADATA);
    }
}
