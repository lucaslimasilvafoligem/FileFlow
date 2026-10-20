package file.infrastructure.record;

import java.io.IOException;
import java.io.InputStream;

/** Entrada do upload, independente de Spring MVC. {@code content} pode ser aberto várias vezes.*/
public record UploadFileCommand(String originalName, long size, ContentSource content) {
    @FunctionalInterface
    public interface ContentSource {InputStream open() throws IOException;}
}
