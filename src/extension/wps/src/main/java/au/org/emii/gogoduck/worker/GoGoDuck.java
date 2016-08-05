package au.org.emii.gogoduck.worker;

import au.org.emii.gogoduck.exception.GoGoDuckException;
import au.org.emii.utils.GoGoDuckConfig;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private UserLog userLog = null;
    private ProgressListener progressListener = null;
    private String mimeType = "application/x-netcdf";
    private String extension = "nc";
    private GoGoDuckConfig goGoDuckConfig;
    private URLMangler urlMangler;

    public GoGoDuck(Catalog catalog, String profile, String subset, String outputFile, String format, GoGoDuckConfig goGoDuckConfig) {
        this.profile = profile;
        this.subset = subset;
        this.outputFile = new File(outputFile).toPath();
        this.format = format;
        this.limit = goGoDuckConfig.getFileLimit();
        this.baseTmpDir = new File(System.getProperty("java.io.tmpdir")).toPath();
        this.userLog = new UserLog();
        this.indexReader = new FeatureSourceIndexReader(this.userLog, catalog);
        this.goGoDuckConfig = goGoDuckConfig;
        setUrlMangler(profile, goGoDuckConfig);
    }

    private void setUrlMangler(String profile, GoGoDuckConfig goGoDuckConfig) {
        try {
            Map<String, String> urlMangling = new HashMap<>();
            urlMangling.putAll(goGoDuckConfig.getUrlSubstitution(profile));
            this.urlMangler = new URLMangler(urlMangling);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void setTmpDir(String tmpDir) {
        this.baseTmpDir = new File(tmpDir).toPath();
    }

    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    private synchronized  boolean isCancelled() {
        return null != progressListener && progressListener.isCanceled();
    }

    private void throwIfCancelled() throws GoGoDuckException {
        if (isCancelled())
            throw new GoGoDuckException("Job cancelled");
    }

    public Path run() {
        GoGoDuckModule module = new GoGoDuckModule(profile, indexReader, subset, goGoDuckConfig);

        Path tmpDir = null;

        try {
            tmpDir = Files.createTempDirectory(baseTmpDir, "gogoduck");

            URIList URIList = module.getUriList();

            enforceFileLimit(URIList, limit);
            downloadFiles(URIList, tmpDir);
            throwIfCancelled();
            File tempDirFile = new File(tmpDir.toString());
            File[] files = tempDirFile.listFiles();
            if (files != null && files.length > 0) {
                module.loadFileMetadata(files[0]);
            }
            applySubsetMultiThread(tmpDir, module, goGoDuckConfig.getThreadCount());
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
            String errMsg = String.format("Your aggregation failed! Reason for failure is: '%s'", e.getMessage());
            userLog.log(errMsg);
            logger.error(errMsg, e);
            throw new GoGoDuckException(e.getMessage());
        }
        finally {
            cleanTmpDir(tmpDir);
            userLog.close();
        }
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
        URL url = urlMangler.mangle(uri);
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

    private void applySubsetSingleFileNcks(File file, GoGoDuckModule module) {
        logger.info("Applying Subsetting to single file");

        try {
            File tmpFile = File.createTempFile("tmp", ".nc");

            List<String> command = new ArrayList<>();
            command.add(goGoDuckConfig.getNcksPath());
            command.add("-a");
            command.add("-4");
            command.add("-O");
            if (!module.isTimeUnlimited()) {
                command.add("--mk_rec_dmn");
                command.add("time");
            }

            command.addAll(module.getSubsetParameters().getNcksParameters());
            command.addAll(module.getExtraParameters());

            command.add(file.getPath());
            command.add(tmpFile.getPath());

            logger.info(String.format("Applying subset '%s' to '%s'", module.getExtraParameters(), file.toPath()));
            execute(command);

            Files.delete(file.toPath());
            Files.move(tmpFile.toPath(), file.toPath());
        }
        catch (Exception e) {
            throw new GoGoDuckException(String.format("Could not apply subset to file '%s': '%s'", file.getPath(), e.getMessage()));
        }
    }

    private static class NcksRunnable implements Runnable {
        private static final Logger logger = LoggerFactory.getLogger(GoGoDuck.class);

        private File file;
        private GoGoDuckModule module = null;
        private GoGoDuck gogoduck = null;

        NcksRunnable(File file, GoGoDuckModule module, GoGoDuck gogoduck) {
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
            gogoduck.applySubsetSingleFileNcks(file, module);
        }
    }

    private void applySubsetMultiThread(Path tmpDir, GoGoDuckModule module, int threadCount) throws GoGoDuckException {
        logger.info(String.format("Applying subset on directory '%s'", tmpDir));
        List<String> ncksSubsetParameters;

        try {
            File[] directoryListing = tmpDir.toFile().listFiles();
            if (directoryListing != null) {
                ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

                for (File file : directoryListing) {
                    ncksSubsetParameters = module.getSubsetParameters().getNcksParameters();
                    logger.info(String.format("Subset for operation is '%s'", ncksSubsetParameters));
                    userLog.log(String.format("Applying subset '%s'", ncksSubsetParameters));
                    executorService.submit(new NcksRunnable(file, module, this));
                }

                executorService.shutdown();
                executorService.awaitTermination(MAX_EXECUTION_TIME_DAYS, TimeUnit.DAYS);
            } else {
                throw new GoGoDuckException("No files available for sub-setting");
            }
        } catch (InterruptedException e) {
            throw new GoGoDuckException("Task interrupted while waiting to complete", e);
        } catch (Exception e) {
            throw new GoGoDuckException(e.getMessage(), e);
        }
    }

    private void postProcess(Path tmpDir, GoGoDuckModule module) throws GoGoDuckException {
        File[] directoryListing = tmpDir.toFile().listFiles();

        if (directoryListing != null) {
            for (File file : directoryListing) {
                try {
                    if (!module.unpackNetcdf()) {
                        return;
                    }

                    File tmpFile = File.createTempFile("ncpdq", ".nc");

                    List<String> command = new ArrayList<>();
                    command.add(goGoDuckConfig.getNcpdqPath());
                    command.add("-O");
                    command.add("-U");
                    command.add(file.getAbsolutePath());
                    command.add(tmpFile.getAbsolutePath());

                    logger.info(String.format("Unpacking file (ncpdq) '%s' to '%s'", file.toPath(), tmpFile.toPath()));
                    GoGoDuck.execute(command);

                    Files.delete(file.toPath());
                    Files.move(tmpFile.toPath(), file.toPath());
                } catch (Exception e) {
                    throw new GoGoDuckException(String.format("Could not run ncpdq on file '%s'", file.toPath()));
                }
            }
        } else {
            throw new GoGoDuckException("No files available for post processing");
        }
    }

    private void aggregate(Path tmpDir, Path outputFile) throws GoGoDuckException {
        File[] directoryListing = tmpDir.toFile().listFiles();

        if (directoryListing != null) {

            List<String> command = new ArrayList<>();
            command.add(goGoDuckConfig.getNcrcatPath());
            command.add("-D2");
            command.add("-4");
            command.add("-h");
            command.add("-O");

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
        } else {
            throw new GoGoDuckException("No files present for aggregation");
        }
    }

    private void updateMetadata(GoGoDuckModule module, Path outputFile) throws Exception {
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
