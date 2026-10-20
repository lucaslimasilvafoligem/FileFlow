package storage.infrastructure.record;

/** Resultado de um upload. */
public record StorageObject(String key, String etag, long size) {}
