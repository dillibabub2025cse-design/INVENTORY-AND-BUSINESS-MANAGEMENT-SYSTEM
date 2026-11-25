package exception;

/**
 * Thrown when database operations or transactional commits encounter errors.
 */
public class DatabaseOperationException extends AppException {
    public DatabaseOperationException(String message) {
        super(message);
    }

    public DatabaseOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}

