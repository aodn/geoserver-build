package au.org.emii.gogoduck.worker;

public class GoGoDuckException extends RuntimeException {
    GoGoDuckException(String reason) {
        super(reason);
    }
}
