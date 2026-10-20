package file.infrastructure.dto.response;

import java.time.Instant;

import file.infrastructure.record.DownloadLink;

public record DownloadLinkResponse(String url, Instant expiresAt) {
    public static DownloadLinkResponse from(DownloadLink link) {
        return new DownloadLinkResponse(link.url(), link.expiresAt());
    }
}
