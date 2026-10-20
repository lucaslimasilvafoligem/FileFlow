package file.infrastructure.contract;

import file.infrastructure.dto.response.DownloadLinkResponse;
import file.infrastructure.dto.response.FileResponse;
import file.infrastructure.enumerate.FileSortField;
import file.infrastructure.enumerate.FileStatus;
import file.infrastructure.enumerate.SortDirection;
import file.infrastructure.record.PageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

public interface FileApiInterface {
    ResponseEntity<FileResponse> upload(@RequestPart("file") MultipartFile file);

    PageResponse<FileResponse> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "CREATED_AT") FileSortField sort,
            @RequestParam(defaultValue = "DESC") SortDirection direction,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) FileStatus status,
            @RequestParam(required = false) Long minSize,
            @RequestParam(required = false) Long maxSize,
            @RequestParam(required = false) Instant createdFrom,
            @RequestParam(required = false) Instant createdTo
    );

    FileResponse get(@PathVariable UUID id);

    ResponseEntity<DownloadLinkResponse> downloadDefault(@PathVariable UUID id);

    ResponseEntity<DownloadLinkResponse> downloadPresigned(@PathVariable UUID id);

    ResponseEntity<StreamingResponseBody> downloadStream(@PathVariable UUID id);

    ResponseEntity<Void> delete(@PathVariable UUID id);
}
