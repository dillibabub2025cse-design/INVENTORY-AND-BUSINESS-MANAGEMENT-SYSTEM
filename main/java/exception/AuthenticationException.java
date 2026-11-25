package exception;

/**
 * Thrown when user authentication fails or access privileges are violated.
 */
public class AuthenticationException extends AppException {
    public AuthenticationException(String message) {
        super(message);
    }
}

