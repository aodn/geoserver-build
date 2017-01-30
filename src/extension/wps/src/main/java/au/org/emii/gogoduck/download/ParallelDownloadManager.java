package au.org.emii.gogoduck.download;

import au.org.emii.gogoduck.util.Ordinal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Manages the parallel download of a list of files to local storage
 * limiting the amount of local storage used by only downloading up
 * to the configured storage limit and then waiting until previously
 * downloaded files are accessed and deleted before downloading
 * more files.
 *
 * Downloads are returned in the order provided, blocking
 * if necessary until the download has been completed
 *
 * Example usage:
 *
 *     try (
 *         ParallelDownloadManager downloadManager =
 *             new ParallelDownloadManager()
 *     ) {
 *         for (Download download : downloadManager.download(requests)) {
 *             if (download instanceof DownloadError) {
 *                 continue;
 *             } else {
 *                 // do something with the download
 *             }
 *             downloadManager.remove();  // removes current download
 *         }
 *     }
 *
 * Use the Config class to override default configuration e.g.
 *
 *     Config config = new Config.ConfigBuilder()
 *                          .downloadDirectory(tmpDir)
 *                          .poolSize(132);
 *     ParallelDownloadManager downloadManager = new ParallelDownloadManager(config)
 */

public class ParallelDownloadManager implements Iterable<Download>, Iterator<Download>, AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(ParallelDownloadManager.class);
    private final ExecutorService pool;
    private final Config config;
    private final LinkedList<DownloadRequest> unactionedQueue;
    private final LinkedList<Future<Download>> inProgressQueue;
    private final Downloader downloader;
    private Download previous = null;
    private long localStorageAllocated = 0L;

    public ParallelDownloadManager(Config config) {
        this(config, new Downloader(config.getConnectTimeOut(), config.getReadTimeOut()));
    }

    protected ParallelDownloadManager(Config config, Downloader downloader) {
        this.pool = Executors.newFixedThreadPool(config.getPoolSize());
        this.config = config;
        this.unactionedQueue = new LinkedList<>();
        this.inProgressQueue = new LinkedList<>();
        this.downloader = downloader;
    }

    public Iterable<Download> download(Set<DownloadRequest> downloadRequests) {
        unactionedQueue.addAll(downloadRequests);
        downloadUpToStorageLimit();
        return this;
    }

    // Iterable methods

    @Override
    public Iterator<Download> iterator() {
        return this;
    }

    // Iterator methods

    @Override
    public boolean hasNext() {
        return unactionedQueue.size() > 0 || inProgressQueue.size() > 0;
    }

    @Override
    public Download next() {
        try {
            Future<Download> next = inProgressQueue.remove();
            Download result = next.get(); // blocks until the next download is complete
            previous = result;
            return result;
        } catch (InterruptedException|ExecutionException e) {
            throw new RuntimeException("Unexpected system error retrieving download", e);
        }
    }

    @Override
    public void remove() {
        try {
            logger.debug(String.format("Deleting download from %s", previous.getURL()));
            Files.deleteIfExists(previous.getPath());
            localStorageAllocated -= previous.getSize();
            downloadUpToStorageLimit();
        } catch (IOException e) {
            throw new RuntimeException(
                String.format("Unexpected system error: unable to delete downloaded file %s", previous.getPath()), e);
        }
    }

    @Override
    public void close() {
        pool.shutdown(); // Disable new tasks from being submitted

        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                pool.shutdownNow(); // Cancel currently executing tasks

                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                    throw new RuntimeException("Download thread pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    private void downloadUpToStorageLimit() {
        DownloadRequest next = unactionedQueue.peek();

        while (next != null && (localStorageAllocated == 0 || wontExceedLocalStorageLimit(next))) {
            final DownloadRequest request = unactionedQueue.pop();
            logger.debug(String.format("Adding %s to download queue", request.getUrl()));
            inProgressQueue.add(pool.submit(new DownloadCallable(request)));
            localStorageAllocated += request.getSize();
            next = unactionedQueue.peek();
        }
    }

    private boolean wontExceedLocalStorageLimit(DownloadRequest next) {
        return localStorageAllocated + next.getSize() < config.getLocalStorageLimit();
    }

    private class DownloadCallable implements Callable<Download> {
        private final DownloadRequest request;

        public DownloadCallable(DownloadRequest request) {
            this.request = request;
        }

        @Override
        public Download call() {
            logger.debug(String.format("Downloading %s", request.getUrl()));
            int attempt = 1;
            Download result = download();
            while (result instanceof DownloadError && ++attempt <= config.getDownloadAttempts()) {
                logger.debug(
                    String.format("Downloading %s %s%s attempt", request.getUrl(), attempt, Ordinal.suffix(attempt)));
                result = download();
            }
            if (result instanceof DownloadError) {
                logger.warn(String.format("Failed to download %s after %d tries", request.getUrl(), config.getDownloadAttempts()));
            } else {
                logger.debug(String.format("Downloaded %s", request.getUrl()));
            }
            return result;
        }

        private Download download() {
            Path path = config.getDownloadDirectory().resolve(UUID.randomUUID().toString());

            try {
                downloader.download(request.getUrl(), path);
                return new Download(request.getUrl(), path, request.getSize());
            } catch (IOException e) {
                logger.warn(String.format("Could not download %s", request.getUrl()), e);
                return new DownloadError(request.getUrl(), path, request.getSize(), e);
            }
        }
    }

}
