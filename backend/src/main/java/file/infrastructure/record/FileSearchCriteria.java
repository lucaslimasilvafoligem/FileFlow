package file.infrastructure.record;

import java.time.Instant;

import file.infrastructure.enumerate.FileSortField;
import file.infrastructure.enumerate.FileStatus;
import file.infrastructure.enumerate.SortDirection;

/** Filtros, ordenação e paginação da listagem. {@code contentTypePrefix} aceita "image/" ou "image/png".*/
public record FileSearchCriteria(
        String query,
        String contentTypePrefix,
        FileStatus status,
        Long minSize,
        Long maxSize,
        Instant createdFrom,
        Instant createdTo,
        FileSortField sort,
        SortDirection direction,
        int page,
        int size
) {}
