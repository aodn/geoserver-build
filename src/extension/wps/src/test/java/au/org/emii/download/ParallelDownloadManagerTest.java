package au.org.emii.download;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;

public class ParallelDownloadManagerTest {

    private static final long KiB = 1024L;
    private static final int MILLISECONDS_IN_A_SECOND = 1000;

    private Path downloadDir;
    private Downloader downloader;
    private ExecutorService pool;

    @Before
    public void setup() throws IOException {
        downloadDir = Files.createTempDirectory("download.");
        downloader = new Downloader(60*MILLISECONDS_IN_A_SECOND, 60*MILLISECONDS_IN_A_SECOND);
        pool = Executors.newFixedThreadPool(8);
    }

    @Test
    public void returnedInOrder() throws IOException {
        DownloadRequest[] testRequests = {
            new DownloadRequest(new URL("http://s3-ap-southeast-2.amazonaws.com/imos-data/IMOS/ACORN/gridded_1h-avg-current-map_QC/ROT/2016/08/20/IMOS_ACORN_V_20160820T013000Z_ROT_FV01_1-hour-avg.nc"), 133778L),
            new DownloadRequest(new URL("http://s3-ap-southeast-2.amazonaws.com/imos-data/IMOS/ACORN/gridded_1h-avg-current-map_QC/ROT/2016/08/20/IMOS_ACORN_V_20160820T003000Z_ROT_FV01_1-hour-avg.nc"), 132578L),
            new DownloadRequest(new URL("http://s3-ap-southeast-2.amazonaws.com/imos-data/IMOS/ACORN/gridded_1h-avg-current-map_QC/ROT/2016/08/20/IMOS_ACORN_V_20160820T183000Z_ROT_FV01_1-hour-avg.nc"), 119607L),
            new DownloadRequest(new URL("http://s3-ap-southeast-2.amazonaws.com/imos-data/IMOS/ACORN/gridded_1h-avg-current-map_QC/ROT/2016/08/20/IMOS_ACORN_V_20160820T023000Z_ROT_FV01_1-hour-avg.nc"), 136305L),
            new DownloadRequest(new URL("http://s3-ap-southeast-2.amazonaws.com/imos-data/IMOS/ACORN/gridded_1h-avg-current-map_QC/ROT/2016/08/20/IMOS_ACORN_V_20160820T213000Z_ROT_FV01_1-hour-avg.nc"), 128654L)
        };

        List<Download> downloadResults = new ArrayList<Download>();

        DownloadConfig config = new DownloadConfig.ConfigBuilder()
            .downloadDirectory(downloadDir)
            .build();

        ParallelDownloadManager manager = new ParallelDownloadManager(config, downloader, pool);

        int i = 0;

        for (Download download: manager.download(toSet(testRequests))) {
            downloadResults.add(download);
            assertEquals(true, Files.exists(download.getPath()));
            assertEquals(testRequests[i].getUrl(), download.getURL());
            assertEquals(testRequests[i].getSize(), Files.size(download.getPath()));
            manager.remove();
            i++;
        }

        assertEquals(5, downloadResults.size());
    }

    @Test
    public void waitsUntilSpaceFreed() throws IOException, InterruptedException {
        DownloadRequest[] testRequests = {
            new DownloadRequest(new URL("http://s3-ap-southeast-2.amazonaws.com/imos-data/IMOS/ACORN/gridded_1h-avg-current-map_QC/ROT/2016/08/20/IMOS_ACORN_V_20160820T013000Z_ROT_FV01_1-hour-avg.nc"), 133778L),
            new DownloadRequest(new URL("http://s3-ap-southeast-2.amazonaws.com/imos-data/IMOS/ACORN/gridded_1h-avg-current-map_QC/ROT/2016/08/20/IMOS_ACORN_V_20160820T003000Z_ROT_FV01_1-hour-avg.nc"), 132578L),
            new DownloadRequest(new URL("http://s3-ap-southeast-2.amazonaws.com/imos-data/IMOS/ACORN/gridded_1h-avg-current-map_QC/ROT/2016/08/20/IMOS_ACORN_V_20160820T183000Z_ROT_FV01_1-hour-avg.nc"), 119607L),
            new DownloadRequest(new URL("http://s3-ap-southeast-2.amazonaws.com/imos-data/IMOS/ACORN/gridded_1h-avg-current-map_QC/ROT/2016/08/20/IMOS_ACORN_V_20160820T023000Z_ROT_FV01_1-hour-avg.nc"), 136305L),
            new DownloadRequest(new URL("http://s3-ap-southeast-2.amazonaws.com/imos-data/IMOS/ACORN/gridded_1h-avg-current-map_QC/ROT/2016/08/20/IMOS_ACORN_V_20160820T213000Z_ROT_FV01_1-hour-avg.nc"), 128654L)
        };

        List<Download> downloadResults = new ArrayList<Download>();

        DownloadConfig config = new DownloadConfig.ConfigBuilder()
            .downloadDirectory(downloadDir)
            .localStorageLimit(150 * KiB)
            .build();

        ParallelDownloadManager manager = new ParallelDownloadManager(config, downloader, pool);

        for (Download download: manager.download(toSet(testRequests))) {
            downloadResults.add(download);
            assertEquals(1, downloadDir.toFile().list().length);
            Thread.sleep(100);
            manager.remove();
        }

        assertEquals(5, downloadResults.size());
    }

    @Test
    public void downloadsLargeFiles() throws MalformedURLException {
        DownloadRequest[] testRequests = {
            new DownloadRequest(new URL("http://s3-ap-southeast-2.amazonaws.com/imos-data/IMOS/ACORN/gridded_1h-avg-current-map_QC/ROT/2016/08/20/IMOS_ACORN_V_20160820T013000Z_ROT_FV01_1-hour-avg.nc"), 133778L),
            new DownloadRequest(new URL("http://s3-ap-southeast-2.amazonaws.com/imos-data/IMOS/ACORN/gridded_1h-avg-current-map_QC/ROT/2016/08/20/IMOS_ACORN_V_20160820T003000Z_ROT_FV01_1-hour-avg.nc"), 132578L),
            new DownloadRequest(new URL("http://s3-ap-southeast-2.amazonaws.com/imos-data/IMOS/ACORN/gridded_1h-avg-current-map_QC/ROT/2016/08/20/IMOS_ACORN_V_20160820T183000Z_ROT_FV01_1-hour-avg.nc"), 119607L),
            new DownloadRequest(new URL("http://s3-ap-southeast-2.amazonaws.com/imos-data/IMOS/ACORN/gridded_1h-avg-current-map_QC/ROT/2016/08/20/IMOS_ACORN_V_20160820T023000Z_ROT_FV01_1-hour-avg.nc"), 136305L),
            new DownloadRequest(new URL("http://s3-ap-southeast-2.amazonaws.com/imos-data/IMOS/ACORN/gridded_1h-avg-current-map_QC/ROT/2016/08/20/IMOS_ACORN_V_20160820T213000Z_ROT_FV01_1-hour-avg.nc"), 128654L)
        };

        List<Download> downloadResults = new ArrayList<Download>();

        DownloadConfig config = new DownloadConfig.ConfigBuilder()
            .downloadDirectory(downloadDir)
            .localStorageLimit(1 * KiB)
            .build();

        ParallelDownloadManager manager = new ParallelDownloadManager(config, downloader, pool);

        for (Download download: manager.download(toSet(testRequests))) {
            downloadResults.add(download);
            manager.remove();
        }

        assertEquals(5, downloadResults.size());
    }

    @Test(expected = DownloadException.class)
    public void throwsExceptionOnDownloadError() throws MalformedURLException {
        DownloadRequest[] testRequests = {
            new DownloadRequest(new URL("http://s3-ap-southeast-2.amazonaws.com/imos-data/IMOS/ACORN/gridded_1h-avg-current-map_QC/ROT/2016/08/20/IMOS_ACORN_V_20160820T013000Z_ROT_FV01_1-hour-avg.nc"), 133778L),
            new DownloadRequest(new URL("http://s3-ap-southeast-2.amazonaws.com/imos-data/IMOS/BROKEN/gridded_1h-avg-current-map_QC/ROT/2016/08/20/IMOS_ACORN_V_20160820T003000Z_ROT_FV01_1-hour-avg.nc"), 132578L),
            new DownloadRequest(new URL("http://s3-ap-southeast-2.amazonaws.com/imos-data/IMOS/ACORN/gridded_1h-avg-current-map_QC/ROT/2016/08/20/IMOS_ACORN_V_20160820T183000Z_ROT_FV01_1-hour-avg.nc"), 119607L),
            new DownloadRequest(new URL("http://s3-ap-southeast-2.amazonaws.com/imos-data/IMOS/ACORN/gridded_1h-avg-current-map_QC/ROT/2016/08/20/IMOS_ACORN_V_20160820T023000Z_ROT_FV01_1-hour-avg.nc"), 136305L),
            new DownloadRequest(new URL("http://s3-ap-southeast-2.amazonaws.com/imos-data/IMOS/ACORN/gridded_1h-avg-current-map_QC/ROT/2016/08/20/IMOS_ACORN_V_20160820T213000Z_ROT_FV01_1-hour-avg.nc"), 128654L)
        };

        List<Download> downloadResults = new ArrayList<Download>();

        DownloadConfig config = new DownloadConfig.ConfigBuilder()
            .downloadDirectory(downloadDir)
            .localStorageLimit(1 * KiB)
            .retryInterval(1)
            .build();

        ParallelDownloadManager manager = new ParallelDownloadManager(config, downloader, pool);

        for (Download download: manager.download(toSet(testRequests))) {
            downloadResults.add(download);
            manager.remove();
        }

        assertEquals(5, downloadResults.size());
        assertEquals(true, downloadResults.get(1) instanceof DownloadError);

    }

    @Test
    public void retriesFailedDownloads() throws IOException {
        DownloadRequest[] testRequests = {
            new DownloadRequest(new URL("http://s3-ap-southeast-2.amazonaws.com/imos-data/IMOS/ACORN/gridded_1h-avg-current-map_QC/ROT/2016/08/20/IMOS_ACORN_V_20160820T213000Z_ROT_FV01_1-hour-avg.nc"), 128654L)
        };

        Downloader downloader = mock(Downloader.class);
        doThrow(new IOException()).when(downloader).download(eq(testRequests[0].getUrl()), any(Path.class));

        DownloadConfig config = new DownloadConfig.ConfigBuilder()
                                    .downloadAttempts(5)
                                    .retryInterval(1)
                                    .build();

        try {
            ParallelDownloadManager manager = new ParallelDownloadManager(config, downloader, pool);

            for (Download download : manager.download(toSet(testRequests))) {
                manager.remove();
            }
        } catch (DownloadException e) {
            // its what we are expecting
        }

        verify(downloader, times(5)).download(eq(testRequests[0].getUrl()), any(Path.class));
    }

    @Test
    public void cleansUpResources() throws MalformedURLException {
        DownloadRequest[] testRequests = {
            new DownloadRequest(new URL("http://s3-ap-southeast-2.amazonaws.com/imos-data/IMOS/ACORN/gridded_1h-avg-current-map_QC/ROT/2016/08/20/IMOS_ACORN_V_20160820T013000Z_ROT_FV01_1-hour-avg.nc"), 133778L),
            new DownloadRequest(new URL("http://s3-ap-southeast-2.amazonaws.com/imos-data/IMOS/ACORN/gridded_1h-avg-current-map_QC/ROT/2016/08/20/IMOS_ACORN_V_20160820T003000Z_ROT_FV01_1-hour-avg.nc"), 132578L),
            new DownloadRequest(new URL("http://s3-ap-southeast-2.amazonaws.com/imos-data/IMOS/ACORN/gridded_1h-avg-current-map_QC/ROT/2016/08/20/IMOS_ACORN_V_20160820T183000Z_ROT_FV01_1-hour-avg.nc"), 119607L),
            new DownloadRequest(new URL("http://s3-ap-southeast-2.amazonaws.com/imos-data/IMOS/ACORN/gridded_1h-avg-current-map_QC/ROT/2016/08/20/IMOS_ACORN_V_20160820T023000Z_ROT_FV01_1-hour-avg.nc"), 136305L),
            new DownloadRequest(new URL("http://s3-ap-southeast-2.amazonaws.com/imos-data/IMOS/ACORN/gridded_1h-avg-current-map_QC/ROT/2016/08/20/IMOS_ACORN_V_20160820T213000Z_ROT_FV01_1-hour-avg.nc"), 128654L)
        };

        List<Download> downloadResults = new ArrayList<Download>();

        DownloadConfig config = new DownloadConfig.ConfigBuilder()
            .downloadDirectory(downloadDir)
            .build();

        ParallelDownloadManager manager = new ParallelDownloadManager(config, downloader, pool);

        for (Download download: manager.download(toSet(testRequests))) {
            downloadResults.add(download);
            assertEquals(true, Files.exists(download.getPath()));
            manager.remove();
        }

        assertEquals(5, downloadResults.size());
        assertEquals(0, downloadDir.toFile().list().length);
    }

    @After
    public void deleteDownloadDir() throws IOException {
        delete(downloadDir.toFile());
        pool.shutdownNow();
    }

    private void delete(File path){
        for (File file : path.listFiles()) {
            if (file.isDirectory()) {
                delete(file);
            } else {
                file.delete();
            }
        }

        path.delete();
    }

    private Set<DownloadRequest> toSet(DownloadRequest[] testRequests) {
        Set<DownloadRequest> result = new LinkedHashSet<>();

        for (DownloadRequest request: testRequests) {
            result.add(request);
        }

        return result;
    }

}
