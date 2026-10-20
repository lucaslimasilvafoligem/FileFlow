package exception.storage;

public class StorageUnavailableException extends StorageException {
    public StorageUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
