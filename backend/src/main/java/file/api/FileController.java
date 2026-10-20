package file.api;

import file.infrastructure.contract.FileApiInterface;
import file.infrastructure.contract.FileApplicationInterface;
import file.infrastructure.dto.response.DownloadLinkResponse;
import file.infrastructure.dto.response.FileResponse;
import file.infrastructure.enumerate.FileSortField;
import file.infrastructure.enumerate.FileStatus;
import file.infrastructure.enumerate.SortDirection;
import file.infrastructure.record.FileContent;
import file.infrastructure.record.FilePage;
import file.infrastructure.record.FileRecord;
import file.infrastructure.record.FileSearchCriteria;
import file.infrastructure.record.PageResponse;
import file.infrastructure.record.UploadFileCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/files")
@Tag(name = "Files")
public class FileController implements FileApiInterface{
    private final FileApplicationInterface service;

    public FileController(FileApplicationInterface service) {
        this.service = service;
    }

    @Override
    @Operation(summary = "Upload de arquivo (multipart, campo 'file')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileResponse> upload(@RequestPart("file") MultipartFile file) {
        FileRecord created = service.upload(
            new UploadFileCommand(
                file.getOriginalFilename(),
                file.getSize(),
                file::getInputStream
            )
        );
        URI location = ServletUriComponentsBuilder
                        .fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(created.id())
                        .toUri();
        return ResponseEntity.created(location).body(FileResponse.from(created));
    }

    @Override
    @Operation(summary = "Lista arquivos com filtros, ordenação e paginação")
    @GetMapping
    public PageResponse<FileResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "CREATED_AT") FileSortField sort,
            @RequestParam(defaultValue = "DESC") SortDirection direction,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) FileStatus status,
            @RequestParam(required = false) Long minSize,
            @RequestParam(required = false) Long maxSize,
            @RequestParam(required = false) Instant createdFrom,
            @RequestParam(required = false) Instant createdTo
        ) {
        FilePage result = service.search(
            new FileSearchCriteria(
                q,
                contentType,
                status,
                minSize,
                maxSize,
                createdFrom,
                createdTo,
                sort,
                direction,
                page,
                size
            )
        );
        return new PageResponse<>(
            result.items().stream().map(FileResponse::from).toList(),
            result.page(),
            result.size(),
            result.totalElements(),
            result.totalPages()
        );
    }

    @Override
    @Operation(summary = "Metadados de um arquivo")
    @GetMapping("/{id}")
    public FileResponse get(@PathVariable UUID id) {
        return FileResponse.from(service.get(id));
    }

    @Override
    @GetMapping(value = "/{id}/download", params = "!strategy")
    public ResponseEntity<DownloadLinkResponse> downloadDefault(@PathVariable UUID id) {
        return ResponseEntity.ok(DownloadLinkResponse.from(service.downloadLink(id)));
    }

    @Override
    @GetMapping(value = "/{id}/download", params = "strategy=PRESIGNED")
    public ResponseEntity<DownloadLinkResponse> downloadPresigned(@PathVariable UUID id) {
        return ResponseEntity.ok(DownloadLinkResponse.from(service.downloadLink(id)));
    }

    @Override
    @GetMapping(value = "/{id}/download", params = "strategy=STREAM")
    public ResponseEntity<StreamingResponseBody> downloadStream(@PathVariable UUID id) {
        FileContent content = service.openContent(id);
        StreamingResponseBody body = out -> {
            try (InputStream in = content.stream()) {in.transferTo(out);}
        };

        return ResponseEntity.ok()
                .contentType(mediaTypeOf(content.file().contentType()))
                .contentLength(content.file().size())
                .header(
                    HttpHeaders.CONTENT_DISPOSITION,
                    ContentDisposition
                        .attachment()
                        .filename(
                            content.file().originalName(),
                            StandardCharsets.UTF_8
                        )
                        .build()
                        .toString()
                )
                .header("X-Content-Type-Options", "nosniff")
                .body(body);
    }

    @Override
    @Operation(summary = "Exclui o arquivo (idempotente: 204 mesmo se já excluído ou inexistente)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private static MediaType mediaTypeOf(String contentType) {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (InvalidMediaTypeException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
