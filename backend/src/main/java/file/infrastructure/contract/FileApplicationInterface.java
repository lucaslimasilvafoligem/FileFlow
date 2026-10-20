package file.infrastructure.contract;

import file.infrastructure.record.DownloadLink;
import file.infrastructure.record.FileContent;
import file.infrastructure.record.FilePage;
import file.infrastructure.record.FileRecord;
import file.infrastructure.record.FileSearchCriteria;
import file.infrastructure.record.UploadFileCommand;

import java.util.UUID;

public interface FileApplicationInterface {
    FileRecord upload(UploadFileCommand command);

    FileRecord get(UUID id);

    FilePage search(FileSearchCriteria criteria);

    DownloadLink downloadLink(UUID id);

    FileContent openContent(UUID id);

    void delete(UUID id);
}
