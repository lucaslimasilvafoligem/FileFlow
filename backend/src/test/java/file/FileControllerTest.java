package file;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.Instant;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import exception.GlobalExceptionHandler;
import exception.file.FileInvalidException;
import exception.storage.StoredFileNotFoundException;
import file.api.FileController;
import file.infrastructure.contract.FileApplicationInterface;
import file.infrastructure.enumerate.FileStatus;
import file.infrastructure.record.DownloadLink;
import file.infrastructure.record.FileRecord;

@WebMvcTest(controllers = FileController.class)
@ContextConfiguration(classes = FileControllerTest.TestApplication.class)
class FileControllerTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(WebTestConfiguration.class)
    static class TestApplication {
    }

    @TestConfiguration
    @EnableWebMvc
    @Import(GlobalExceptionHandler.class)
    static class WebTestConfiguration {
        @Bean
        FileController fileController(FileApplicationInterface service) {
            return new FileController(service);
        }
    }

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private FileApplicationInterface service;

    @Test
    void getUnknownFileReturns404() throws Exception {
        UUID id = UUID.randomUUID();

        when(service.get(id))
                .thenThrow(new StoredFileNotFoundException(id));

        mvc.perform(get("/files/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("File not found"));
    }

    @Test
    void listRejectsOversizedPage() throws Exception {
        mvc.perform(get("/files").param("size", "1000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadOfEmptyFileReturns400() throws Exception {
        when(service.upload(any()))
                .thenThrow(new FileInvalidException("File is empty"));

        mvc.perform(multipart("/files")
                        .file(new MockMultipartFile(
                                "file",
                                "a.txt",
                                "text/plain",
                                new byte[0])))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("File is empty"));
    }

    @Test
    void uploadReturns201WithLocationAndNoInternalKey() throws Exception {
        UUID id = UUID.randomUUID();
        Instant now = Instant.parse("2026-01-01T00:00:00Z");

        when(service.upload(any())).thenReturn(
                new FileRecord(
                        id,
                        null,
                        "objects/" + id + ".txt",
                        "a.txt",
                        "txt",
                        "text/plain",
                        3,
                        "abc",
                        FileStatus.UPLOADED,
                        now,
                        now,
                        null));

        mvc.perform(multipart("/files")
                        .file(new MockMultipartFile(
                                "file",
                                "a.txt",
                                "text/plain",
                                "abc".getBytes())))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        Matchers.endsWith("/files/" + id)))
                .andExpect(jsonPath("$.originalName").value("a.txt"))
                .andExpect(jsonPath("$.objectKey").doesNotExist());
    }

    @Test
    void downloadDefaultsToPresignedLink() throws Exception {
        UUID id = UUID.randomUUID();

        when(service.downloadLink(id)).thenReturn(
                new DownloadLink(
                        "https://signed.example/x",
                        Instant.parse("2026-01-01T00:00:00Z")));

        mvc.perform(get("/files/" + id + "/download"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url")
                        .value("https://signed.example/x"));
    }
}
