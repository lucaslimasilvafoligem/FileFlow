package exception;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import exception.file.FileTooLargeException;
import exception.file.FileInvalidException;
import exception.file.FileUnsupportedTypeException;
import exception.storage.StorageAuthenticationException;
import exception.storage.StorageObjectNotFoundException;
import exception.storage.StorageOperationException;
import exception.storage.StorageUnavailableException;
import exception.storage.StoredFileNotFoundException;
import jakarta.validation.ConstraintViolationException;

/**
 * Respostas de erro em RFC 7807 (ProblemDetail). Erros de infraestrutura nunca vazam detalhes ao cliente.
 * Os erros padrão do MVC (parâmetro inválido, multipart grande demais etc.) vêm da classe base.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(StoredFileNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleFileNotFound(
            StoredFileNotFoundException e) {
        ProblemDetail problem = problem(
                HttpStatus.NOT_FOUND,
                "File not found",
                e.getMessage()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(FileInvalidException.class)
    ProblemDetail handleInvalidFile(FileInvalidException e) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid file", e.getMessage());
    }

    @ExceptionHandler(FileTooLargeException.class)
    ProblemDetail handleTooLarge(FileTooLargeException e) {
        return problem(HttpStatus.PAYLOAD_TOO_LARGE, "File too large", e.getMessage());
    }

    @ExceptionHandler(FileUnsupportedTypeException.class)
    ProblemDetail handleUnsupportedType(FileUnsupportedTypeException e) {
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported file type", e.getMessage());
    }

    // Registro existe no banco mas o objeto sumiu: inconsistência a investigar.
    @ExceptionHandler(StorageObjectNotFoundException.class)
    ProblemDetail handleMissingObject(StorageObjectNotFoundException e) {
        LOG.error("storage.inconsistency: {}", e.getMessage());
        return problem(HttpStatus.NOT_FOUND, "File content not found", "The file content is not available");
    }

    @ExceptionHandler(StorageUnavailableException.class)
    ProblemDetail handleStorageUnavailable(StorageUnavailableException e) {
        LOG.warn("storage.unavailable: {}", e.getMessage());
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Storage unavailable", "Storage is temporarily unavailable");
    }

    // Credencial inválida é problema de configuração do servidor, não do cliente.    
    @ExceptionHandler({StorageAuthenticationException.class, StorageOperationException.class})
    ProblemDetail handleStorageFailure(RuntimeException e) {
        LOG.error("storage.failure", e);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error", "Unexpected error");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(ConstraintViolationException e) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Invalid request",
                "One or more request parameters are invalid"
        );
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception e) {
        LOG.error("unexpected.error", e);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error", "Unexpected error");
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
