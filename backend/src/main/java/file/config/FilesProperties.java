package file.config;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

/**
 * Configuração do módulo de arquivos.
 * @param upload política de upload
 */
@Validated
@ConfigurationProperties(prefix = "files")
public record FilesProperties(@Valid @DefaultValue Upload upload) {

    /**
     * Política de upload.
     * @param maxSize tamanho máximo por arquivo
     * @param blockedContentTypes tipos (detectados pelo conteúdo) que nunca são aceitos
     * @param blockedExtensions extensões que nunca são aceitas
     */
    public record Upload(
            @DefaultValue("500MB") DataSize maxSize,
            @DefaultValue({
                "application/x-msdownload",
                "application/x-dosexec",
                "application/x-executable",
                "application/x-sharedlib",
                "application/x-mach-binary",
                "application/x-sh",
                "application/java-archive"
            }) List<String> blockedContentTypes,
            @DefaultValue({
                "exe", "dll", "bat", "cmd", "com", "msi", "scr", "sh", "ps1", "vbs", "jar", "php", "jsp"
            }) List<String> blockedExtensions) {
    }
}
