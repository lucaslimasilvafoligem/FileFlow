package exception.file;

public class FileTooLargeException extends RuntimeException {
    public FileTooLargeException(long maxBytes) {
        super("File exceeds the maximum size of " + maxBytes + " bytes");
    }
}
