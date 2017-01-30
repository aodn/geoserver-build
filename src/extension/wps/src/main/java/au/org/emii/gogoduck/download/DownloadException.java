package au.org.emii.gogoduck.download;

/**
 * Exception thrown when an error occurs downloading a file
 */
public class DownloadException extends RuntimeException {
    public DownloadException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
