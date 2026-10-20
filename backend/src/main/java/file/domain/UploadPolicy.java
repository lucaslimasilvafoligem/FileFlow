package file.domain;

import file.config.FilesProperties;
import exception.file.FileTooLargeException;
import exception.file.FileInvalidException;
import exception.file.FileUnsupportedTypeException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Regras de aceitação e saneamento de upload. O nome original nunca vira caminho no storage.*/
@Component
public class UploadPolicy {
    private static final Pattern INVISIBLE = Pattern.compile("[\\p{Cc}\\p{Cf}]");
    private static final Pattern SAFE_EXTENSION = Pattern.compile("[a-z0-9]{1,16}");
    private static final int MAX_NAME_LENGTH = 255;
    private static final String DEFAULT_NAME = "file";
    private final long maxSizeBytes;
    private final Set<String> blockedContentTypes;
    private final Set<String> blockedExtensions;

    public UploadPolicy(FilesProperties properties) {
        FilesProperties.Upload upload = properties.upload();
        this.maxSizeBytes = upload.maxSize().toBytes();
        this.blockedContentTypes = lowerCase(upload.blockedContentTypes());
        this.blockedExtensions = lowerCase(upload.blockedExtensions());
    }

    public void validateSize(long size) {
        if (size <= 0) {
            throw new FileInvalidException("File is empty");
        }
        if (size > maxSizeBytes) {
            throw new FileTooLargeException(maxSizeBytes);
        }
    }

    /** Remove caminho, caracteres de controle/formatação (ex.: override bidi) e limita o tamanho.*/
    public String sanitizeName(String rawName) {
        String name = rawName == null ? "" : rawName.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        name = INVISIBLE.matcher(name).replaceAll("").strip();
        if (name.isEmpty() || ".".equals(name) || "..".equals(name)) {
            return DEFAULT_NAME;
        }
        if (name.length() > MAX_NAME_LENGTH) {
            int dot = name.lastIndexOf('.');
            String extension = dot > 0 && name.length() - dot <= 17 ? name.substring(dot) : "";
            name = name.substring(0, MAX_NAME_LENGTH - extension.length()) + extension;
        }
        return name;
    }

    /** Extensão em minúsculas, ou vazio se ausente ou fora do padrão seguro.*/
    public String extensionOf(String sanitizedName) {
        int dot = sanitizedName.lastIndexOf('.');
        if (dot <= 0 || dot == sanitizedName.length() - 1) {
            return "";
        }
        String extension = sanitizedName.substring(dot + 1).toLowerCase(Locale.ROOT);
        return SAFE_EXTENSION.matcher(extension).matches() ? extension : "";
    }

    public void validateType(String detectedContentType, String extension) {
        if (
            blockedContentTypes.contains(detectedContentType.toLowerCase(Locale.ROOT)) ||
            blockedExtensions.contains(extension)
        ) {
            throw new FileUnsupportedTypeException("File type is not allowed");
        }
    }

    private static Set<String> lowerCase(List<String> values) {
        return values.stream().map(v -> v.toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
    }
}
