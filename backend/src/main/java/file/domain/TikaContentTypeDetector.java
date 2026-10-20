package file.domain;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import file.infrastructure.contract.ContentTypeDetectorInterface;

@Component
class TikaContentTypeDetector implements ContentTypeDetectorInterface {
    private final Tika tika = new Tika();
    @Override
    public String detect(InputStream content, String fileName) {
        try {
            return tika.detect(content, fileName);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
