package storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import storage.config.StorageConfig;
import storage.config.StorageProperties;
import storage.domain.S3StorageAdapter;
import storage.infrastructure.record.PageResult;
import storage.infrastructure.record.StorageObjectMetadata;
import storage.infrastructure.record.UploadObject;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import exception.storage.StorageAuthenticationException;
import exception.storage.StorageObjectNotFoundException;
import exception.storage.StorageUnavailableException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Testcontainers
class S3StorageAdapterIT {
    @Container
    static final MinIOContainer MINIO = new MinIOContainer(
        DockerImageName.parse("minio/minio:latest")
    );

    private final List<AutoCloseable> toClose = new ArrayList<>();
    private S3StorageAdapter adapter;

    @BeforeEach
    void setUp() {
        String bucket = "it-" + UUID.randomUUID().toString().substring(0, 8);
        adapter = adapterFor(properties(MINIO.getS3URL(), MINIO.getUserName(), MINIO.getPassword(), bucket));
        adapter.ensureBucket();
    }

    @AfterEach
    void tearDown() throws Exception {
        for (AutoCloseable closeable : toClose) {
            closeable.close();
        }
    }

    @Test
    void ensureBucketIsIdempotentUnderConcurrency() {
        String bucket = "race-" + UUID.randomUUID().toString().substring(0, 8);
        S3StorageAdapter racing = adapterFor(properties(MINIO.getS3URL(), MINIO.getUserName(), MINIO.getPassword(), bucket));

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            futures.add(CompletableFuture.runAsync(racing::ensureBucket));
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

        assertThat(racing.bucketExists()).isTrue();
    }

    @Test
    void uploadThenDownloadRoundTrip() throws Exception {
        byte[] payload = "conteudo de teste".getBytes(StandardCharsets.UTF_8);

        var stored = adapter.upload(new UploadObject("objects/a.txt", new ByteArrayInputStream(payload), payload.length, "text/plain"));

        assertThat(stored.size()).isEqualTo(payload.length);
        assertThat(adapter.getMetadata("objects/a.txt").contentType()).isEqualTo("text/plain");
        try (InputStream in = adapter.download("objects/a.txt")) {
            assertThat(in.readAllBytes()).isEqualTo(payload);
        }
    }

    @Test
    void downloadOfMissingKeyThrowsNotFound() {
        assertThatThrownBy(() -> adapter.download("objects/missing.txt"))
                .isInstanceOf(StorageObjectNotFoundException.class);
    }

    @Test
    void existsAndDeleteAreIdempotent() {
        byte[] payload = {1, 2, 3};
        adapter.upload(new UploadObject("objects/b.bin", new ByteArrayInputStream(payload), payload.length, "application/octet-stream"));
        assertThat(adapter.exists("objects/b.bin")).isTrue();

        adapter.delete("objects/b.bin");
        adapter.delete("objects/b.bin");

        assertThat(adapter.exists("objects/b.bin")).isFalse();
    }

    @Test
    void listPaginatesWithContinuationToken() {
        for (int i = 0; i < 3; i++) {
            byte[] payload = {(byte) i};
            adapter.upload(new UploadObject("p/" + i, new ByteArrayInputStream(payload), 1, "application/octet-stream"));
        }

        PageResult<StorageObjectMetadata> first = adapter.list("p/", null, 2);
        PageResult<StorageObjectMetadata> second = adapter.list("p/", first.nextToken(), 2);

        assertThat(first.items()).hasSize(2);
        assertThat(first.hasNext()).isTrue();
        assertThat(second.items()).hasSize(1);
        assertThat(second.hasNext()).isFalse();
    }

    @Test
    void presignedUrlIsDownloadableWithoutCredentials() throws Exception {
        byte[] payload = "via presigned".getBytes(StandardCharsets.UTF_8);
        adapter.upload(new UploadObject("objects/c.txt", new ByteArrayInputStream(payload), payload.length, "text/plain"));

        String url = adapter.generatePresignedUrl("objects/c.txt", Duration.ofMinutes(1));
        HttpResponse<byte[]> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo(payload);
    }

    @Test
    void wrongCredentialsThrowAuthenticationException() {
        S3StorageAdapter bad = adapterFor(properties(MINIO.getS3URL(), MINIO.getUserName(), "senha-errada-123", "any-bucket"));

        assertThatThrownBy(bad::ping).isInstanceOf(StorageAuthenticationException.class);
    }

    @Test
    void unreachableEndpointThrowsUnavailable() {
        S3StorageAdapter down = adapterFor(properties("http://localhost:1", "x", "y", "any-bucket"));

        assertThatThrownBy(down::ping).isInstanceOf(StorageUnavailableException.class);
    }

    private S3StorageAdapter adapterFor(StorageProperties props) {
        StorageConfig config = new StorageConfig();
        S3Client client = config.s3Client(props);
        S3Presigner presigner = config.s3Presigner(props);
        toClose.add(client);
        toClose.add(presigner);
        return new S3StorageAdapter(client, presigner, props);
    }

    private static StorageProperties properties(String endpoint, String accessKey, String secretKey, String bucket) {
        return new StorageProperties(endpoint, endpoint, "us-east-1", accessKey, secretKey, bucket, true,
                Duration.ofMinutes(15), Duration.ofSeconds(2), Duration.ofSeconds(10), Duration.ofSeconds(8));
    }
}
