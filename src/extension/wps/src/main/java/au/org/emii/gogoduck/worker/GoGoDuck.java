package au.org.emii.gogoduck.worker;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.catalog.Catalog;
import org.opengis.util.ProgressListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

public class GoGoDuck {
    private static final Logger logger = LoggerFactory.getLogger(GoGoDuck.class);

    // Max execution time is determined by geoserver configuration, this is
    // here merely to have a value to satisfy awaitTermination of the JAVA API
    private static final int MAX_EXECUTION_TIME_DAYS = 365;

    private IndexReader indexReader = null;
    private final String profile;
    private final String subset;
    private final String format;
    private Path outputFile;
    private final Integer limit;
    private Path baseTmpDir;
    private int threadCount = 1;
    private UserLog userLog = null;
    private ProgressListener progressListener = null;
    private String mimeType = "application/x-netcdf";
    private String extension = "nc";

    protected GoGoDuck(String profile, String subset, String outputFile, String format, Integer limit) {
        this.profile = profile;
        this.subset = subset;
        this.outputFile = new File(outputFile).toPath();
        this.format = format;
        this.limit = limit;
        this.baseTmpDir = new File(System.getProperty("java.io.tmpdir")).toPath();
        this.userLog = new UserLog();
    }

    public GoGoDuck(String geoserverUrl, String profile, String subset, String outputFile, String format, Integer limit) {
        this(profile, subset, outputFile, format, limit);
        this.indexReader = new HttpIndexReader(this.userLog, geoserverUrl);
    }

    public GoGoDuck(Catalog catalog, String profile, String subset, String outputFile, String format, Integer limit) {
        this(profile, subset, outputFile, format, limit);
        this.indexReader = new FeatureSourceIndexReader(this.userLog, catalog);
    }

    public void setTmpDir(String tmpDir) {
        this.baseTmpDir = new File(tmpDir).toPath();
    }

    public void setThreadCount(int threadCount) {
        logger.info(String.format("Setting thread count to %d", threadCount));
        this.threadCount = threadCount;
    }

    public void setUserLog(String userLog) {
        this.userLog = new UserLog(userLog);
    }

    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    public synchronized  boolean isCancelled() {
        if (null != progressListener) {
            return progressListener.isCanceled();
        }
        else {
            return false;
        }
    }

    private void throwIfCancelled() throws GoGoDuckException {
        if (isCancelled())
            throw new GoGoDuckException("Job cancelled");
    }

    public Path run() {
        GoGoDuckModule module = new GoGoDuckModule(profile, indexReader, subset, userLog);

        Path tmpDir = null;

        try {
            tmpDir = Files.createTempDirectory(baseTmpDir, "gogoduck");

            URIList URIList = module.getUriList();

            enforceFileLimit(URIList, limit);
            downloadFiles(URIList, tmpDir);
            throwIfCancelled();
            applySubsetMultiThread(tmpDir, module, threadCount);
            throwIfCancelled();
            postProcess(tmpDir, module);
            throwIfCancelled();
            aggregate(tmpDir, outputFile);
            throwIfCancelled();
            updateMetadata(module, outputFile);
            throwIfCancelled();
            Converter converter = Converter.newInstance(format);
            outputFile = converter.convert(outputFile);
            mimeType = converter.getMimeType();
            extension = converter.getExtension();
            throwIfCancelled();
            userLog.log("Your aggregation was successful!");
            return outputFile;
        }
        catch (Exception e) {
            userLog.log("Your aggregation failed!");
            userLog.log(String.format("Reason for failure is: '%s'", e.getMessage()));
            logger.error(e.toString());
            throw new GoGoDuckException(e.getMessage());
        }
        finally {
            cleanTmpDir(tmpDir);
            userLog.close();
        }
    }

    public Integer score() {
        return 0;
    }

    private void enforceFileLimit(URIList URIList, Integer limit) throws GoGoDuckException {
        logger.info("Enforcing file limit...");
        if (URIList.size() > limit) {
            userLog.log("Sorry we cannot process this request due to the amount of files requiring processing.");
            userLog.log(String.format("The file limit is '%d' and this aggregation job requires '%d' files.", limit, URIList.size()));
            userLog.log("Please recreate a download request that will require less files to aggregate.");
            logger.error(String.format("Aggregation asked for %d, we allow only %d", URIList.size(), limit));
            throw new GoGoDuckException("Too many files");
        }
        else if (URIList.size() == 0) {
            userLog.log("The list of URLs obtained was empty, were your subseting parameters OK?");
            logger.error("No URLs returned for aggregation");
            throw new GoGoDuckException("No files returned from geoserver");
        }

        // All good - keep going :)
    }

    private static boolean fileExists(File file) {
        return file.exists() && ! file.isDirectory();
    }

    private void createSymbolicLink(File srcFile, Path dst) {
        try {
            logger.info(String.format("Linking '%s' -> '%s'", srcFile, dst));
            Files.createSymbolicLink(dst, srcFile.toPath());
        } catch (IOException e) {
            userLog.log(String.format("Failed accessing '%s'", srcFile));
            throw new GoGoDuckException(e.getMessage());
        }
    }

    private void downloadFile(URI uri, Path dst) {
        URL url = URLMangler.mangle(uri);
        logger.info(String.format("Downloading '%s' -> '%s'", url.toString(), dst));

        try {
            ReadableByteChannel rbc = Channels.newChannel(url.openStream());
            FileOutputStream fos = new FileOutputStream(dst.toFile());
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }
        catch (IOException e) {
            userLog.log(String.format("Failed downloading '%s'", url));
            throw new GoGoDuckException(e.getMessage());
        }
    }

    private void downloadFiles(URIList uriList, Path tmpDir) throws GoGoDuckException {
        logger.info(String.format("Downloading %d file(s)", uriList.size()));

        for (URI uri : uriList) {
            if (isCancelled()) {
                logger.warn("GoGoDuck was cancelled during download.");
                return;
            }

            File srcFile = new File(uriList.get(0).toString());
            String basename = new File(uri.toString()).getName();
            Path dst = new File(tmpDir + File.separator + basename).toPath();

            if (fileExists(srcFile)) {
                createSymbolicLink(srcFile, dst);
            }
            else {
                downloadFile(uri, dst);
            }

            String extension = FilenameUtils.getExtension(dst.getFileName().toString());

            if (extension.equals("gz")) {
                gunzipInPlace(dst.toFile());
            }
        }
    }

    private static void gunzipInPlace(File file) {
        try {
            logger.info(String.format("Gunzipping '%s'", file));
            File gunzipped = File.createTempFile("tmp", ".nc");

            FileInputStream fis = new FileInputStream(file);
            GZIPInputStream gis = new GZIPInputStream(fis);
            FileOutputStream fos = new FileOutputStream(gunzipped);
            byte[] buffer = new byte[8192];
            int len;
            while((len = gis.read(buffer)) != -1){
                fos.write(buffer, 0, len);
            }
            fos.close();
            gis.close();

            Files.delete(file.toPath());
            Files.move(gunzipped.toPath(), file.toPath());
        } catch (IOException e) {
            throw new GoGoDuckException(String.format("Failed gunzip on '%s': '%s'", file, e.getMessage()));
        }
    }

    private static void applySubsetSingleFileNcks(File file, GoGoDuckModule module) {
        List<String> ncksSubsetParameters = module.getNcksSubsetParameters(file.getAbsoluteFile().toString()).getNcksParameters();
        List<String> ncksExtraParameters = module.ncksExtraParameters();

        try {
            File tmpFile = File.createTempFile("tmp", ".nc");

            List<String> command = new ArrayList<String>();
            command.add(GoGoDuckConfig.ncksPath);
            command.add("-a");
            command.add("-4");
            command.add("-O");
            command.addAll(ncksSubsetParameters);
            command.addAll(ncksExtraParameters);

            command.add(file.getPath());
            command.add(tmpFile.getPath());

            logger.info(String.format("Applying subset '%s' to '%s'", ncksSubsetParameters, file.toPath()));
            execute(command);

            Files.delete(file.toPath());
            Files.move(tmpFile.toPath(), file.toPath());
        }
        catch (Exception e) {
            throw new GoGoDuckException(String.format("Could not apply subset to file '%s': '%s'", file.getPath(), e.getMessage()));
        }
    }

    private static void applySubsetSingleFileNative(File file, GoGoDuckModule module) {
        // TODO implement!
    }

    private static class NcksRunnable implements Runnable {
        private static final Logger logger = LoggerFactory.getLogger(GoGoDuck.class);

        private String name;
        private File file;
        private GoGoDuckModule module = null;
        private GoGoDuck gogoduck = null;
        private ProgressListener progressListener = null;

        NcksRunnable(File file, GoGoDuckModule module, GoGoDuck gogoduck) {
            this.name = name;
            this.file = file;
            this.module = module;
            this.gogoduck = gogoduck;
        }

        public void run() {
            if (gogoduck.isCancelled()) {
                gogoduck.userLog.log("Job was cancelled");
                logger.warn("Cancelled by progress listener.");
                return;
            }

            gogoduck.userLog.log(String.format("Processing file '%s'", file.toPath().getFileName()));
            applySubsetSingleFileNcks(file, module);
        }
    }

    private void applySubsetMultiThread(Path tmpDir, GoGoDuckModule module, int threadCount) throws GoGoDuckException {
        logger.info(String.format("Applying subset on directory '%s'", tmpDir));

        File[] directoryListing = tmpDir.toFile().listFiles();

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        for (final File file : directoryListing) {
            executorService.submit(new NcksRunnable(file, module, this));
        }

        executorService.shutdown();
        try {
            executorService.awaitTermination(MAX_EXECUTION_TIME_DAYS, TimeUnit.DAYS);
        }
        catch (InterruptedException e) {
            throw new GoGoDuckException("Task interrupted while waiting to complete", e);
        }
    }

    private static void applySubsetSingleThread(Path tmpDir, GoGoDuckModule module) throws GoGoDuckException {
        logger.info(String.format("Applying subset on directory '%s'", tmpDir));

        File[] directoryListing = tmpDir.toFile().listFiles();

        for (File file : directoryListing) {
            applySubsetSingleFileNcks(file, module);
        }
    }

    private static void postProcess(Path tmpDir, GoGoDuckModule module) throws GoGoDuckException {
        File[] directoryListing = tmpDir.toFile().listFiles();
        for (File file : directoryListing) {
            module.postProcess(file);
        }
    }

    private static void aggregate(Path tmpDir, Path outputFile) throws GoGoDuckException {
        aggregateNcrcat(tmpDir, outputFile);
    }

    private static void aggregateNcrcat(Path tmpDir, Path outputFile) throws GoGoDuckException {
        List<String> command = new ArrayList<String>();
        command.add(GoGoDuckConfig.ncrcatPath);
        command.add("-D2");
        command.add("-4");
        command.add("-h");
        command.add("-O");

        File[] directoryListing = tmpDir.toFile().listFiles();
        if (directoryListing.length == 1) {
            // Special case where we have only 1 file
            File file = directoryListing[0];
            try {
                if (outputFile.toFile().exists() && outputFile.toFile().isFile()) {
                    logger.info(String.format("Deleting '%s'", outputFile));
                    Files.delete(outputFile);
                }
                logger.info(String.format("Renaming '%s' -> '%s'", file, outputFile));
                Files.move(file.toPath(), outputFile);
            }
            catch (IOException e) {
                logger.error(e.toString());
                throw new GoGoDuckException(String.format("Could not rename result file: '%s'", e.getMessage()));
            }
        }
        else {
            logger.info(String.format("Concatenating %d files into '%s'", directoryListing.length, outputFile));
            for (File file : directoryListing) {
                command.add(file.getAbsolutePath());
            }
            command.add(outputFile.toFile().getAbsolutePath());

            // Running ncrcat
            try {
                execute(command);
            }
            catch (Exception e) {
                throw new GoGoDuckException(String.format("Could not concatenate files into a single file: '%s'", e.getMessage()));
            }
        }
    }

    private static void aggregateJava(Path tmpDir, Path outputFile) throws GoGoDuckException {
        // TODO implmenet!
    }

    private static void updateMetadata(GoGoDuckModule module, Path outputFile) {
        module.updateMetadata(outputFile);
    }

    private static void cleanTmpDir(Path tmpDir) {
        logger.debug(String.format("Removing temporary directory '%s'", tmpDir));
        try {
            FileUtils.deleteDirectory(tmpDir.toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int execute(List<String> command) throws Exception {
        logger.info(command.toString());

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Map<String, String> environ = builder.environment();

        final Process process = builder.start();
        InputStream is = process.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line;
        while ((line = br.readLine()) != null) {
            logger.info(line);
        }

        try {
            process.waitFor();
        }
        catch (InterruptedException e) {
            logger.error(String.format("Interrupted: '%s'", e.getMessage()));
            throw e;
        }

        return process.exitValue();
    }

    public String getMimeType() { return mimeType; }

    public String getExtension() { return extension; }
}
