package storage.infrastructure.record;

import java.time.Instant;

/** Metadados de um objeto. {@code contentType} é nulo em resultados de listagem. */
public record StorageObjectMetadata(String key, long size, String contentType, String etag, Instant lastModified) {}
