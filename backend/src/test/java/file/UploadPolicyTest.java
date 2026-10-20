package file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import exception.file.FileTooLargeException;
import exception.file.FileInvalidException;
import exception.file.FileUnsupportedTypeException;
import file.config.FilesProperties;
import file.domain.UploadPolicy;

class UploadPolicyTest {

    private final UploadPolicy policy = new UploadPolicy(new FilesProperties(new FilesProperties.Upload(
            DataSize.ofMegabytes(10), List.of("application/x-msdownload"), List.of("exe"))));

    @Test
    void sanitizeNameDropsPathTraversal() {
        assertThat(policy.sanitizeName("../../etc/passwd")).isEqualTo("passwd");
        assertThat(policy.sanitizeName("C:\\Users\\x\\foto.png")).isEqualTo("foto.png");
    }

    @Test
    void sanitizeNameDropsControlAndBidiCharacters() {
        assertThat(policy.sanitizeName("re\u0000lat\u202Eorio.pdf")).isEqualTo("relatorio.pdf");
    }

    @Test
    void sanitizeNameFallsBackToDefault() {
        assertThat(policy.sanitizeName(null)).isEqualTo("file");
        assertThat(policy.sanitizeName("..")).isEqualTo("file");
        assertThat(policy.sanitizeName("   ")).isEqualTo("file");
    }

    @Test
    void sanitizeNameTruncatesKeepingExtension() {
        String result = policy.sanitizeName("a".repeat(300) + ".pdf");

        assertThat(result).hasSize(255).endsWith(".pdf");
    }

    @Test
    void extensionIsLowercasedAndOnlyTheLastSegment() {
        assertThat(policy.extensionOf("Foto.JPG")).isEqualTo("jpg");
        assertThat(policy.extensionOf("archive.tar.gz")).isEqualTo("gz");
    }

    @Test
    void extensionIsEmptyWhenMissingOrUnsafe() {
        assertThat(policy.extensionOf(".bashrc")).isEmpty();
        assertThat(policy.extensionOf("noext")).isEmpty();
        assertThat(policy.extensionOf("trailing.")).isEmpty();
        assertThat(policy.extensionOf("x.p@ng")).isEmpty();
    }

    @Test
    void validateSizeRejectsEmptyAndOversized() {
        assertThatThrownBy(() -> policy.validateSize(0)).isInstanceOf(FileInvalidException.class);
        assertThatThrownBy(() -> policy.validateSize(DataSize.ofMegabytes(10).toBytes() + 1))
                .isInstanceOf(FileTooLargeException.class);
        assertThatCode(() -> policy.validateSize(DataSize.ofMegabytes(10).toBytes())).doesNotThrowAnyException();
    }

    @Test
    void validateTypeRejectsBlockedContentTypeAndExtension() {
        assertThatThrownBy(() -> policy.validateType("application/x-msdownload", "bin"))
                .isInstanceOf(FileUnsupportedTypeException.class);
        assertThatThrownBy(() -> policy.validateType("image/png", "exe"))
                .isInstanceOf(FileUnsupportedTypeException.class);
        assertThatCode(() -> policy.validateType("image/png", "png")).doesNotThrowAnyException();
    }
}
