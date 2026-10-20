package file.infrastructure.record;

import java.util.UUID;

/** Evento interno: arquivo gravado (storage + banco). Outros módulos reagem sem que files os conheça. */
public record FileStored(UUID fileId, String contentType, long size) {}
