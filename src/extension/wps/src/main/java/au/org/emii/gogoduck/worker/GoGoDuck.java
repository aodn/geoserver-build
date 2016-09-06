package au.org.emii.gogoduck.worker;

import au.org.emii.gogoduck.exception.GoGoDuckException;
import au.org.emii.utils.GoGoDuckConfig;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.catalog.Catalog;
import org.opengis.util.ProgressListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.Attribute;

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
        this.indexReader = new FeatureSourceIndexReader(catalog);
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

        FileMetadata fileMetadata = new FileMetadata(profile, indexReader, subset, goGoDuckConfig);

        Path tmpDir = null;

        try {
            tmpDir = Files.createTempDirectory(baseTmpDir, "gogoduck");

            URIList URIList = fileMetadata.getUriList();

            enforceFileLimits(URIList, limit, goGoDuckConfig.getFileSizeLimit());
            List<Path> downloadedFiles = downloadFiles(URIList, tmpDir);
            throwIfCancelled();
            fileMetadata.load(downloadedFiles.get(0).toFile());
            applySubsetMultiThread(downloadedFiles, fileMetadata, goGoDuckConfig.getThreadCount());
            throwIfCancelled();
            postProcess(downloadedFiles, fileMetadata);
            throwIfCancelled();
            aggregate(downloadedFiles, outputFile);
            throwIfCancelled();
            updateMetadata(fileMetadata, outputFile);
            throwIfCancelled();
            Converter converter = Converter.newInstance(format);
            outputFile = converter.convert(outputFile, fileMetadata);
            mimeType = converter.getMimeType();
            extension = converter.getExtension();
            throwIfCancelled();
            return outputFile;
        }
        catch (Exception e) {
            String errMsg = String.format("Your aggregation failed! Reason for failure is: '%s'", e.getMessage());
            logger.error(errMsg, e);
            throw new GoGoDuckException(e.getMessage());
        }
        finally {
            cleanTmpDir(tmpDir);
        }
    }

    private void enforceFileLimits(URIList URIList, Integer limit, double fileSizeLimit) throws GoGoDuckException {
        logger.info("Enforcing file limits...");
        if (URIList.size() > limit) {
            logger.error(String.format("Aggregation asked for %d, we allow only %d", URIList.size(), limit));
            throw new GoGoDuckException("Too many files");
        }
        else if (URIList.size() == 0) {
            logger.error("No URLs returned for aggregation");
            throw new GoGoDuckException("No files returned from geoserver");
        }

        if (fileSizeLimit != 0.0 && URIList.getTotalFileSize() != 0.0 && URIList.getTotalFileSize() > fileSizeLimit) {
            throw new GoGoDuckException(String.format("Total file size %s bytes for %s files, exceeds the limit %s bytes", URIList.getTotalFileSize(), URIList.size(), fileSizeLimit));
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
            throw new GoGoDuckException(e.getMessage());
        }
    }

    private void downloadFile(URI uri, Path dst) {
        URL url = urlMangler.mangle(uri);
        logger.info(String.format("Downloading '%s' -> '%s'", url.toString(), dst));

        try (
            InputStream downloadStream = url.openStream();
            ReadableByteChannel rbc = Channels.newChannel(downloadStream);
            FileOutputStream fos = new FileOutputStream(dst.toFile());
        ) {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }
        catch (IOException e) {
            throw new GoGoDuckException(e.getMessage());
        }
    }

    private List<Path> downloadFiles(URIList uriList, Path tmpDir) throws GoGoDuckException {
        logger.info(String.format("Downloading %d file(s)", uriList.size()));

        List<Path> downloadedFiles = new ArrayList<>();

        for (URI uri : uriList) {
            if (isCancelled()) {
                logger.warn("GoGoDuck was cancelled during download.");
                return null;
            }

            File srcFile = new File(uriList.first().toString());
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

            downloadedFiles.add(dst);
        }

        return downloadedFiles;
    }

    private static void gunzipInPlace(File file) {
        try {
            logger.info(String.format("Gunzipping '%s'", file));
            File gunzipped = File.createTempFile("tmp", ".nc");

            try (
                FileInputStream fis = new FileInputStream(file);
                GZIPInputStream gis = new GZIPInputStream(fis);
                FileOutputStream fos = new FileOutputStream(gunzipped);
            ) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = gis.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
            }

            Files.delete(file.toPath());
            Files.move(gunzipped.toPath(), file.toPath());
        } catch (IOException e) {
            throw new GoGoDuckException(String.format("Failed gunzip on '%s': '%s'", file, e.getMessage()));
        }
    }

    private void applySubsetSingleFileNcks(File file, FileMetadata fileMetadata) {
        logger.info("Applying Subsetting to single file");

        try {
            File tmpFile = File.createTempFile("tmp", ".nc");

            List<String> command = new ArrayList<>();
            command.add(goGoDuckConfig.getNcksPath());
            command.add("-a");
            command.add("-4");
            command.add("-O");
            if (!fileMetadata.isTimeUnlimited()) {
                command.add("--mk_rec_dmn");
                command.add(fileMetadata.getTime().getFullName());
            }

            command.addAll(fileMetadata.getSubsetParameters().getNcksParameters());
            command.addAll(fileMetadata.getExtraParameters());

            command.add(file.getPath());
            command.add(tmpFile.getPath());

            logger.info(String.format("Applying subset '%s' to '%s'", fileMetadata.getExtraParameters(), file.toPath()));
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
        private FileMetadata fileMetadata = null;
        private GoGoDuck gogoduck = null;

        NcksRunnable(File file, FileMetadata fileMetadata, GoGoDuck gogoduck) {
            this.file = file;
            this.fileMetadata = fileMetadata;
            this.gogoduck = gogoduck;
        }

        public void run() {
            if (gogoduck.isCancelled()) {
                logger.warn("Cancelled by progress listener.");
                return;
            }

            gogoduck.applySubsetSingleFileNcks(file, fileMetadata);
        }
    }

    private void applySubsetMultiThread(List<Path> files, FileMetadata fileMetadata, int threadCount) throws GoGoDuckException {
        logger.info(String.format("Applying subset on %d downloaded files", files.size()));

        try {
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

            for (Path file : files) {
                executorService.submit(new NcksRunnable(file.toFile(), fileMetadata, this));
            }

            executorService.shutdown();
            executorService.awaitTermination(MAX_EXECUTION_TIME_DAYS, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            throw new GoGoDuckException("Task interrupted while waiting to complete", e);
        } catch (Exception e) {
            throw new GoGoDuckException(e.getMessage(), e);
        }
    }

    private void postProcess(List<Path> files, FileMetadata fileMetadata) throws GoGoDuckException {
        for (Path file : files) {
            try {
                if (!fileMetadata.unpackNetcdf()) {
                    logger.info(String.format("Not unpacking file %s", file));
                    return;
                }

                File tmpFile = File.createTempFile("ncpdq", ".nc");
                logger.info(String.format("Unpacking file (ncpdq) '%s' to '%s'", file, tmpFile.toPath()));

                List<String> command = new ArrayList<>();
                command.add(goGoDuckConfig.getNcpdqPath());
                command.add("-O");
                command.add("-U");
                command.add(file.toAbsolutePath().toString());
                command.add(tmpFile.getAbsolutePath());

                GoGoDuck.execute(command);

                Files.delete(file);
                Files.move(tmpFile.toPath(), file);
            } catch (Exception e) {
                throw new GoGoDuckException(String.format("Could not run ncpdq on file '%s'", file));
            }
        }
    }

    private void aggregate(List<Path> files, Path outputFile) throws GoGoDuckException {
        List<String> command = new ArrayList<>();
        command.add(goGoDuckConfig.getNcrcatPath());
        command.add("-D2");
        command.add("-4");
        command.add("-h");
        command.add("-O");

        if (files.size() == 1) {
            // Special case where we have only 1 file
            File file = files.get(0).toFile();
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
            logger.info(String.format("Concatenating %d files into '%s'", files.size(), outputFile));
            for (Path file : files) {
                command.add(file.toAbsolutePath().toString());
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

    private void updateMetadata(FileMetadata fileMetadata, Path outputFile) throws Exception {
        try {
            final List<Attribute> globalAttributesToUpdate = fileMetadata.getGlobalAttributesToUpdate(outputFile);
            for (Attribute newAttr : globalAttributesToUpdate) {
                List<String> command = new ArrayList<>();
                command.add(goGoDuckConfig.getNcattedPath());
                command.add("-O");
                command.add("-h");
                command.add("-a");
                command.add(String.format("%s,global,o,c,%s", newAttr.getFullName(), newAttr.getStringValue()));
                command.add(outputFile.toAbsolutePath().toString());
                execute(command);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new GoGoDuckException(String.format("Failed updating metadata for file '%s': '%s'", outputFile, e.getMessage()));
        }
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

        try (
            InputStream is = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr)
        ) {
            String line;
            while ((line = br.readLine()) != null) {
                logger.info(line);
            }

            process.waitFor();

            return process.exitValue();
        }
        catch (InterruptedException e) {
            logger.error(String.format("Interrupted: '%s'", e.getMessage()));
            throw e;
        }
    }

    public String getMimeType() { return mimeType; }

    public String getExtension() { return extension; }
}
