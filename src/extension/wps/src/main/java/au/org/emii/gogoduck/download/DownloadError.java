package au.org.emii.gogoduck.download;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

/**
 * Download error metadata
 */
public class DownloadError extends Download {
    private final Throwable throwable;

    public DownloadError(URL url, Path path, long size, Throwable throwable) {
        super(url,path, size);
        this.throwable = throwable;
    }

    public Throwable getCause() {
        return throwable;
    }
}
