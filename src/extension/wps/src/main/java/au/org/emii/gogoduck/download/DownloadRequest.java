package au.org.emii.gogoduck.download;

import java.net.URL;

/**
 * Requested download metadata
 */
public class DownloadRequest {
    private final URL url;
    private final long size;

    public DownloadRequest(URL url, long size) {
        this.url = url;
        this.size = size;
    }

    public URL getUrl() {
        return url;
    }

    public long getSize() {
        return size;
    }

}
