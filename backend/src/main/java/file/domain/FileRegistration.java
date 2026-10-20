package file.domain;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import file.infrastructure.record.FileRecord;
import file.infrastructure.record.FileStored;
import file.infrastructure.repository.FileRepository;

/**
 * Registra o arquivo no banco. O evento {@link FileStored} é tratado de forma síncrona dentro desta mesma
 * transação: arquivo, jobs e outbox são gravados juntos ou nenhum deles.
 */
@Component
public class FileRegistration {
    private final FileRepository files;
    private final ApplicationEventPublisher events;

    public FileRegistration(FileRepository files, ApplicationEventPublisher events) {
        this.files = files;
        this.events = events;
    }

    @Transactional
    public FileRecord register(FileRecord record) {
        FileRecord saved = files.add(record);
        events.publishEvent(new FileStored(saved.id(), saved.contentType(), saved.size()));
        return saved;
    }
}
