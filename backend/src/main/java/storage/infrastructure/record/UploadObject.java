package storage.infrastructure.record;

import java.io.InputStream;
import java.util.Objects;

/** Dados de um upload. O tamanho é obrigatório para o S3 (sem buffer em memória). */
public record UploadObject(String key, InputStream content, long contentLength, String contentType) {
    public UploadObject {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        Objects.requireNonNull(content, "content");
        if (contentLength < 0) {
            throw new IllegalArgumentException("contentLength must be >= 0");
        }
    }
}
