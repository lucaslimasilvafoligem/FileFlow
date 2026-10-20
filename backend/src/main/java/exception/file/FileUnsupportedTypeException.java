package exception.file;

public class FileUnsupportedTypeException extends RuntimeException {
    public FileUnsupportedTypeException(String message) {
        super(message);
    }
}
