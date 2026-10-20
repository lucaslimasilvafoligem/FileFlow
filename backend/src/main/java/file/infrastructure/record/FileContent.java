package file.infrastructure.record;

import java.io.InputStream;
/** Arquivo + stream do conteúdo. Quem recebe deve fechar o stream. */
public record FileContent(FileRecord file, InputStream stream) {}
