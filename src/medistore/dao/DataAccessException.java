package medistore.dao;

/**
 * Wraps the checked SQLException so screens can stay readable.
 *
 * Forms catch this one type, show the message and carry on rather than
 * repeating try/catch blocks around every single query.
 */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message, Throwable cause) {
        super(message + " (" + cause.getMessage() + ")", cause);
    }

    public DataAccessException(String message) {
        super(message);
    }
}
