package file.application;

import file.domain.FileRegistration;
import file.infrastructure.contract.ContentTypeDetectorInterface;
import file.infrastructure.contract.FileApplicationInterface;
import file.infrastructure.enumerate.FileStatus;
import file.infrastructure.record.DownloadLink;
import file.infrastructure.record.FileContent;
import file.infrastructure.record.FilePage;
import file.infrastructure.record.FileRecord;
import file.infrastructure.record.FileSearchCriteria;
import file.infrastructure.record.UploadFileCommand;
import file.infrastructure.repository.FileRepository;
import file.domain.UploadPolicy;
import storage.config.StorageProperties;
import storage.infrastructure.contract.StoragePort;
import storage.infrastructure.record.UploadObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import exception.storage.StoredFileNotFoundException;

/**
 * Casos de uso de arquivos. Não há transação envolvendo o storage: banco e S3 não compartilham
 * atomicidade, então cada passo é seguro de repetir e falhas parciais são compensadas.
 */
@Service
public class FileApplicationService implements FileApplicationInterface{
    private static final Logger LOG = LoggerFactory.getLogger(FileApplicationService.class);
    private final FileRepository files;
    private final FileRegistration registration;
    private final StoragePort storage;
    private final ContentTypeDetectorInterface detector;
    private final UploadPolicy policy;
    private final StorageProperties storageProperties;
    private final Clock clock;

    public FileApplicationService(
        FileRepository files,
        FileRegistration registration,
        StoragePort storage,
        ContentTypeDetectorInterface detector,
        UploadPolicy policy,
        StorageProperties storageProperties, 
        Clock clock
    ) {
        this.files = files;
        this.registration = registration;
        this.storage = storage;
        this.detector = detector;
        this.policy = policy;
        this.storageProperties = storageProperties;
        this.clock = clock;
    }

    @Override
    public FileRecord upload(UploadFileCommand command) {
        policy.validateSize(command.size());

        String originalName = policy.sanitizeName(command.originalName());
        String extension = policy.extensionOf(originalName);
        String contentType = detectContentType(command, originalName);
        policy.validateType(contentType, extension);

        UUID id = UUID.randomUUID();
        String objectKey = "objects/" + id + (extension.isEmpty() ? "" : "." + extension);
        String checksum = sha256(command);

        try (InputStream in = open(command)) {
            storage.upload(new UploadObject(objectKey, in, command.size(), contentType));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        Instant now = clock.instant();
        FileRecord record = new FileRecord(id, null, objectKey, originalName,
                extension.isEmpty() ? null : extension, contentType, command.size(), checksum,
                FileStatus.UPLOADED, now, now, null);
        try {
            FileRecord saved = registration.register(record);
            LOG.info("files.upload ok id={} size={} contentType={}", id, command.size(), contentType);
            return saved;
        } catch (RuntimeException e) {
            compensate(objectKey, e);
            throw e;
        }
    }

    @Override
    public FileRecord get(UUID id) {
        return files.findActiveById(id).orElseThrow(() -> new StoredFileNotFoundException(id));
    }

    @Override
    public FilePage search(FileSearchCriteria criteria) {
        return files.search(criteria);
    }

    @Override
    public DownloadLink downloadLink(UUID id) {
        FileRecord file = get(id);
        Duration ttl = storageProperties.presignedUrlExpiration();
        String url = storage.generatePresignedUrl(file.objectKey(), ttl, file.originalName());
        return new DownloadLink(url, clock.instant().plus(ttl));
    }

    @Override
    public FileContent openContent(UUID id) {
        FileRecord file = get(id);
        return new FileContent(file, storage.download(file.objectKey()));
    }

    /**
     * Idempotente. Marca o registro e sempre tenta remover o objeto: se o storage falhou numa chamada
     * anterior, o retry conclui a limpeza.
     */
    @Override
    public void delete(UUID id) {
        Optional<FileRecord> found = files.findById(id);
        if (found.isEmpty()) {
            return;
        }
        FileRecord file = found.get();
        if (!file.isDeleted()) {
            files.markDeleted(id, clock.instant());
        }
        storage.delete(file.objectKey());
        LOG.info("files.delete ok id={}", id);
    }

    private void compensate(String objectKey, RuntimeException cause) {
        try {
            storage.delete(objectKey);
        } catch (RuntimeException e) {
            LOG.warn("files.upload compensation failed key={}: orphan object needs reconciliation", objectKey);
            cause.addSuppressed(e);
        }
    }

    private String detectContentType(UploadFileCommand command, String fileName) {
        try (InputStream in = open(command)) {
            return detector.detect(in, fileName);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String sha256(UploadFileCommand command) {
        MessageDigest digest = newSha256();
        byte[] buffer = new byte[8192];
        try (InputStream in = open(command)) {
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static MessageDigest newSha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static InputStream open(UploadFileCommand command) {
        try {
            return command.content().open();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
