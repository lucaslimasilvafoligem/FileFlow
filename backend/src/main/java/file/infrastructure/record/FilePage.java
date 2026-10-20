package file.infrastructure.record;

import java.util.List;

public record FilePage(List<FileRecord> items, int page, int size, long totalElements, int totalPages) {}
