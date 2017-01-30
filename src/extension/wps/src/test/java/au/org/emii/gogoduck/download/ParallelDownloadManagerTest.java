package au.org.emii.gogoduck.download;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import static org.junit.Assert.assertEquals;

public class ParallelDownloadManagerTest {

    private static final long KiB = 1024L;
    private static final long MiB = 1024L * KiB;

    private Path downloadDir;

    @Before
    public void createDownloadDir() throws IOException {
        downloadDir = Files.createTempDirectory("download.");
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

        Config config = new Config.ConfigBuilder()
            .setDownloadDirectory(downloadDir)
            .build();

        try (ParallelDownloadManager manager = new ParallelDownloadManager(config)) {
            int i = 0;
            for (Download download: manager.download(toSet(testRequests))) {
                downloadResults.add(download);
                assertEquals(true, Files.exists(download.getPath()));
                assertEquals(testRequests[i].getUrl(), download.getURL());
                assertEquals(testRequests[i].getSize(), Files.size(download.getPath()));
                i++;
            }
            manager.remove();
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

        Config config = new Config.ConfigBuilder()
            .setDownloadDirectory(downloadDir)
            .setLocalStorageLimit(150 * KiB)
            .build();

        try (ParallelDownloadManager manager = new ParallelDownloadManager(config)) {
            for (Download download: manager.download(toSet(testRequests))) {
                downloadResults.add(download);
                assertEquals(1, downloadDir.toFile().list().length);
                Thread.sleep(100);
                manager.remove();
            }
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

        Config config = new Config.ConfigBuilder()
            .setDownloadDirectory(downloadDir)
            .setLocalStorageLimit(1 * KiB)
            .build();

        try (ParallelDownloadManager manager = new ParallelDownloadManager(config)) {
            for (Download download: manager.download(toSet(testRequests))) {
                downloadResults.add(download);
                manager.remove();
            }
        }

        assertEquals(5, downloadResults.size());
    }

    @Test
    public void returnsErrorsEncountered() throws MalformedURLException {
        DownloadRequest[] testRequests = {
            new DownloadRequest(new URL("http://s3-ap-southeast-2.amazonaws.com/imos-data/IMOS/ACORN/gridded_1h-avg-current-map_QC/ROT/2016/08/20/IMOS_ACORN_V_20160820T013000Z_ROT_FV01_1-hour-avg.nc"), 133778L),
            new DownloadRequest(new URL("http://s3-ap-southeast-2.amazonaws.com/imos-data/IMOS/BROKEN/gridded_1h-avg-current-map_QC/ROT/2016/08/20/IMOS_ACORN_V_20160820T003000Z_ROT_FV01_1-hour-avg.nc"), 132578L),
            new DownloadRequest(new URL("http://s3-ap-southeast-2.amazonaws.com/imos-data/IMOS/ACORN/gridded_1h-avg-current-map_QC/ROT/2016/08/20/IMOS_ACORN_V_20160820T183000Z_ROT_FV01_1-hour-avg.nc"), 119607L),
            new DownloadRequest(new URL("http://s3-ap-southeast-2.amazonaws.com/imos-data/IMOS/ACORN/gridded_1h-avg-current-map_QC/ROT/2016/08/20/IMOS_ACORN_V_20160820T023000Z_ROT_FV01_1-hour-avg.nc"), 136305L),
            new DownloadRequest(new URL("http://s3-ap-southeast-2.amazonaws.com/imos-data/IMOS/ACORN/gridded_1h-avg-current-map_QC/ROT/2016/08/20/IMOS_ACORN_V_20160820T213000Z_ROT_FV01_1-hour-avg.nc"), 128654L)
        };

        List<Download> downloadResults = new ArrayList<Download>();

        Config config = new Config.ConfigBuilder()
            .setDownloadDirectory(downloadDir)
            .setLocalStorageLimit(1 * KiB)
            .build();

        try (ParallelDownloadManager manager = new ParallelDownloadManager(config)) {
            for (Download download: manager.download(toSet(testRequests))) {
                downloadResults.add(download);
                manager.remove();
            }
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

        Config config = new Config.ConfigBuilder().setDownloadAttempts(5).build();

        try (ParallelDownloadManager manager = new ParallelDownloadManager(config, downloader)) {
            for (Download download: manager.download(toSet(testRequests))) {
                manager.remove();
            }
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

        Config config = new Config.ConfigBuilder()
            .setDownloadDirectory(downloadDir)
            .build();

        int currentThreadCount = Thread.activeCount();

        try (ParallelDownloadManager manager = new ParallelDownloadManager(config)) {
            for (Download download: manager.download(toSet(testRequests))) {
                downloadResults.add(download);
                assertEquals(true, Files.exists(download.getPath()));
                manager.remove();
            }
        }

        assertEquals(5, downloadResults.size());
        assertEquals(0, downloadDir.toFile().list().length);
        assertEquals(currentThreadCount, Thread.activeCount());
    }

    @After
    public void deleteDownloadDir() throws IOException {
        delete(downloadDir.toFile());
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
