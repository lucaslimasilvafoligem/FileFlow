package exception.storage;

public class StorageOperationException extends StorageException {
    public StorageOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
