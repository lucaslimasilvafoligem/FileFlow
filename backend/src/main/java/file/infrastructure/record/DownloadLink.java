package file.infrastructure.record;

import java.time.Instant;

public record DownloadLink(String url, Instant expiresAt) {}
