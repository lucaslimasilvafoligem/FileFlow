package exception.storage;

import java.util.UUID;

public class StoredFileNotFoundException extends RuntimeException {
    public StoredFileNotFoundException(UUID id) {
        super("File not found: " + id);
    }
}
