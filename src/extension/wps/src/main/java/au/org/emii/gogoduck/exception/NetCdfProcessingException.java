package au.org.emii.gogoduck.exception;

public class NetCdfProcessingException extends GoGoDuckException {
    public NetCdfProcessingException(String reason) {
        super(reason);
    }

    public NetCdfProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
