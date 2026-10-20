package exception.storage;

public class StorageObjectNotFoundException extends StorageException {
    public StorageObjectNotFoundException(String key, Throwable cause) {
        super("Storage object not found: " + key, cause);
    }
}
