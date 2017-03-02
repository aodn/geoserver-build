package au.org.emii.wps.gogoduck.exception;

public class GoGoDuckException extends RuntimeException {
    public GoGoDuckException(String reason) {
        super(reason);
    }

    public GoGoDuckException(String message, Throwable cause) {
        super(message, cause);
    }
}
