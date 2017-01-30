package au.org.emii.gogoduck.download;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

/**
 * Download error metadata
 */
public class DownloadError extends Download {
    private final IOException exception;

    public DownloadError(URL url, Path path, long size, IOException e) {
        super(url,path, size);
        this.exception = e;
    }

    public IOException getException() {
        return exception;
    }
}
