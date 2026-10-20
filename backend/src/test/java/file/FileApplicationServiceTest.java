package file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import storage.config.StorageProperties;
import storage.infrastructure.contract.StoragePort;
import storage.infrastructure.record.UploadObject;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.unit.DataSize;
import exception.file.FileUnsupportedTypeException;
import file.application.FileApplicationService;
import file.config.FilesProperties;
import file.domain.FileRegistration;
import file.domain.UploadPolicy;
import file.infrastructure.contract.ContentTypeDetectorInterface;
import file.infrastructure.contract.FileApplicationInterface;
import file.infrastructure.enumerate.FileStatus;
import file.infrastructure.record.FileRecord;
import file.infrastructure.record.UploadFileCommand;
import file.infrastructure.repository.FileRepository;

@ExtendWith(MockitoExtension.class)
class FileApplicationServiceTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final String SHA256_OF_ABC = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";
    @Mock
    private FileRepository files;
    @Mock
    private StoragePort storage;
    @Mock
    private ContentTypeDetectorInterface detector;
    @Mock
    private FileRegistration registration;
    private FileApplicationInterface service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        UploadPolicy policy = new UploadPolicy(new FilesProperties(new FilesProperties.Upload(
                DataSize.ofMegabytes(10), List.of("application/x-msdownload"), List.of("exe"))));
        StorageProperties storageProperties = new StorageProperties("http://x", "http://x", "us-east-1", "a", "b",
                "bucket", true, Duration.ofMinutes(5), Duration.ofSeconds(1), Duration.ofSeconds(1),
                Duration.ofSeconds(1));
                service = new FileApplicationService(files, registration, storage, detector, policy, storageProperties,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void uploadStoresObjectWithSafeKeyAndPersistsRecord() {
        byte[] data = "abc".getBytes(StandardCharsets.UTF_8);
        when(detector.detect(any(InputStream.class), eq("foto.jpg"))).thenReturn("image/jpeg");
        when(registration.register(any(FileRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FileRecord result = service.upload(
                new UploadFileCommand("../foto.jpg", data.length, () -> new ByteArrayInputStream(data)));

        ArgumentCaptor<UploadObject> uploaded = ArgumentCaptor.forClass(UploadObject.class);
        verify(storage).upload(uploaded.capture());
        assertThat(uploaded.getValue().key()).matches("objects/[0-9a-f-]{36}\\.jpg");
        assertThat(uploaded.getValue().contentType()).isEqualTo("image/jpeg");
        assertThat(result.originalName()).isEqualTo("foto.jpg");
        assertThat(result.extension()).isEqualTo("jpg");
        assertThat(result.checksum()).isEqualTo(SHA256_OF_ABC);
        assertThat(result.status()).isEqualTo(FileStatus.UPLOADED);
    }

    @Test
    void uploadRemovesStoredObjectWhenDatabaseFails() {
        byte[] data = {1, 2, 3};
        when(detector.detect(any(InputStream.class), eq("a.bin"))).thenReturn("application/octet-stream");
        when(registration.register(any(FileRecord.class)))
                .thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> service.upload(
                new UploadFileCommand("a.bin", data.length, () -> new ByteArrayInputStream(data))))
                .hasMessage("db down");

        verify(storage).delete(startsWith("objects/"));
    }

    @Test
    void uploadRejectsBlockedTypeBeforeTouchingStorage() {
        byte[] data = {1, 2, 3};
        when(detector.detect(any(InputStream.class), eq("a.bin"))).thenReturn("application/x-msdownload");

        assertThatThrownBy(() -> service.upload(
                new UploadFileCommand("a.bin", data.length, () -> new ByteArrayInputStream(data))))
                .isInstanceOf(FileUnsupportedTypeException.class);

        verifyNoInteractions(storage, files);
    }

    @Test
    void deleteOfUnknownFileIsNoOp() {
        UUID id = UUID.randomUUID();
        when(files.findById(id)).thenReturn(Optional.empty());

        service.delete(id);

        verifyNoInteractions(storage);
        verify(files, never()).markDeleted(any(), any());
    }

    @Test
    void deleteOfActiveFileMarksThenRemovesObject() {
        UUID id = UUID.randomUUID();
        when(files.findById(id)).thenReturn(Optional.of(record(id, null)));

        service.delete(id);

        var order = inOrder(files, storage);
        order.verify(files).markDeleted(id, NOW);
        order.verify(storage).delete("objects/" + id + ".txt");
    }

    @Test
    void deleteOfAlreadyDeletedFileStillRetriesStorageCleanup() {
        UUID id = UUID.randomUUID();
        when(files.findById(id)).thenReturn(Optional.of(record(id, NOW.minusSeconds(60))));

        service.delete(id);

        verify(files, never()).markDeleted(any(), any());
        verify(storage).delete("objects/" + id + ".txt");
    }

    private static FileRecord record(UUID id, Instant deletedAt) {
        return new FileRecord(id, null, "objects/" + id + ".txt", "a.txt", "txt", "text/plain", 3, "x",
                FileStatus.UPLOADED, NOW, NOW, deletedAt);
    }
}
