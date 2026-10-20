package exception.file;

public class FileInvalidException extends RuntimeException {
    public FileInvalidException(String message) {
        super(message);
    }
}
