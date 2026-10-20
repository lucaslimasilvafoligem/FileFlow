package processing.domain;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;
import processing.config.ProcessingProperties;
import processing.infrastructure.enumerate.JobType;

/** Decide quais jobs um arquivo gera, a partir do tipo detectado e dos tipos habilitados. */
@Component
public class JobPlanner {
    private final Set<JobType> enabled = EnumSet.noneOf(JobType.class);

    public JobPlanner(ProcessingProperties properties) {
        enabled.addAll(properties.enabledJobTypes());
    }

    public List<JobType> plan(String contentType) {
        String type = contentType.toLowerCase(Locale.ROOT);
        List<JobType> wanted = new ArrayList<>();
        wanted.add(JobType.FILE_METADATA);
        if (type.startsWith("image/")) {
            wanted.add(JobType.IMAGE_THUMBNAIL);
        } else if (type.startsWith("audio/")) {
            wanted.add(JobType.AUDIO_TRANSCRIPTION);
        } else if (type.startsWith("video/")) {
            wanted.add(JobType.VIDEO_METADATA);
            wanted.add(JobType.VIDEO_THUMBNAIL);
            wanted.add(JobType.VIDEO_AUDIO_EXTRACTION);
        } else if (isDocument(type)) {
            wanted.add(JobType.DOCUMENT_TEXT_EXTRACTION);
        }
        return wanted.stream().filter(enabled::contains).toList();
    }

    private static boolean isDocument(String type) {
        return "application/pdf".equals(type)
                || "application/msword".equals(type)
                || type.startsWith("application/vnd.openxmlformats-officedocument.");
    }
}
