package au.org.emii.gogoduck.worker;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opengis.util.ProgressListener;

import java.io.IOException;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class GoGoDuck {
    private static final Logger logger = LoggerFactory.getLogger(GoGoDuck.class);

    private static final Map<String, String> replacePrefixes = new HashMap<String, String>();
    static {
        replacePrefixes.put("/mnt/imos-t3/", "https://data.aodn.org.au/");
        replacePrefixes.put("/mnt/opendap/2/SRS/", "https://thredds.aodn.org.au/thredds/fileServer/srs/");
    }

    public static final String ncksPath = "/usr/bin/ncks";
    public static final String ncrcatPath = "/usr/bin/ncrcat";
    public static final String ncdpqPath = "/usr/bin/ncpdq";

    private final String geoserver;
    private final String profile;
    private final String subset;
    private final Path outputFile;
    private final Integer limit;
    private Path baseTmpDir;
    private int threadCount = 1;
    private UserLog userLog = null;
    private ProgressListener progressListener = null;

    public GoGoDuck(String geoserver, String profile, String subset, String outputFile, Integer limit) {
        this.geoserver = geoserver;
        this.profile = profile;
        this.subset = subset;
        this.outputFile = new File(outputFile).toPath();
        this.limit = limit;
        this.baseTmpDir = new File(System.getProperty("java.io.tmpdir")).toPath();
        this.userLog = new UserLog();
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

    public boolean isCancelled() {
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

    public void run() {
        GoGoDuckModule module = getProfileModule(profile, geoserver, subset, userLog);

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
            userLog.log("Your aggregation was successful!");
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

            if(srcFile.exists() && !srcFile.isDirectory()) {
                Path src = new File(uri.toString()).toPath();

                try {
                    logger.info(String.format("Linking '%s' -> '%s'", src, dst));
                    Files.createSymbolicLink(dst, src);
                } catch (IOException e) {
                    userLog.log(String.format("Failed accessing '%s'", src));
                    throw new GoGoDuckException(e.getMessage());
                }
            }
            else {
                URL url = fileURItoURL(uri);
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

            String extension = FilenameUtils.getExtension(dst.getFileName().toString());

            if (extension.equals("gz")) {
                gunzip(dst.toFile());
            }
        }
    }

    private static URL fileURItoURL(URI uri) {
        try {
            String uriStr = uri.toString();
            for (String key : replacePrefixes.keySet()) {
                if (uriStr.startsWith(key)) {
                    uriStr = uriStr.replace(key, replacePrefixes.get(key));
                }
            }
            return new URL(uriStr);
        }
        catch (MalformedURLException e) {
            throw new GoGoDuckException(e.getMessage());
        }
    }

    private static void gunzip(File file) {
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
        List<String> ncksSubsetParameters = module.getSubsetParameters().getNcksParameters();
        List<String> ncksExtraParameters = module.ncksExtraParameters();

        try {
            File tmpFile = File.createTempFile("tmp", ".nc");

            List<String> command = new ArrayList<String>();
            command.add(ncksPath);
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

    private static class ncksRunnable implements Runnable {
        private static final Logger logger = LoggerFactory.getLogger(GoGoDuck.class);

        private String name;
        private Deque<File> workQueue;
        private GoGoDuckModule module = null;
        private GoGoDuck gogoduck = null;
        private ProgressListener progressListener = null;

        ncksRunnable(String name, Deque<File> workQueue, GoGoDuckModule module, GoGoDuck gogoduck) {
            this.name = name;
            this.workQueue = workQueue;
            this.module = module;
            this.gogoduck = gogoduck;
        }

        public void run() {
            try {
                File file = null;
                while ((file = workQueue.pop()) != null) {
                    if (gogoduck.isCancelled()) {
                        gogoduck.userLog.log("Job was cancelled");
                        logger.warn("Cancelled by progress listener.");
                        return;
                    }
                    gogoduck.userLog.log(String.format("Processing file '%s'", file.toPath().getFileName()));
                    applySubsetSingleFileNcks(file, module);
                }
            }
            catch (NoSuchElementException e) {
                logger.info(String.format("Thread %s finished successfully", name));
            }
        }
    }

    private void applySubsetMultiThread(Path tmpDir, GoGoDuckModule module, int threadCount) throws GoGoDuckException {
        userLog.log(String.format("Applying subset '%s'", module.getSubsetParameters().toString()));
        logger.info(String.format("Applying subset on directory '%s'", tmpDir));

        File[] directoryListing = tmpDir.toFile().listFiles();
        logger.info(String.format("Subset for operation is '%s'", module.getSubsetParameters()));

        Deque<File> workQueue = new ArrayDeque<File>();
        for (File file : directoryListing) {
            workQueue.push(file);
        }

        try {
            Thread[] threads = new Thread[threadCount];
            for (int i = 0; i < threadCount; i++) {
                String threadName = String.format("Subset_%d", i + 1);
                threads[i] = new Thread(new ncksRunnable(threadName, workQueue, module, this));
            }
            for (int i = 0; i < threadCount; i++) {
                threads[i].start();
            }
            for (int i = 0; i < threadCount; i++) {
                threads[i].join();
            }
        }
        catch (InterruptedException e) {
            userLog.log("Failed ");
            throw new GoGoDuckException(String.format("InterruptedException: '%s'", e.getMessage()));
        }
    }

    private static void applySubsetSingleThread(Path tmpDir, GoGoDuckModule module) throws GoGoDuckException {
        logger.info(String.format("Applying subset on directory '%s'", tmpDir));

        File[] directoryListing = tmpDir.toFile().listFiles();
        logger.info(String.format("Subset for operation is '%s'", module.getSubsetParameters()));

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
        command.add(ncrcatPath);
        command.add("-D2");
        command.add("-4");
        command.add("-h");
        command.add("-O");

        File[] directoryListing = tmpDir.toFile().listFiles();
        if (directoryListing.length == 1) {
            // Special case where we have only 1 file
            File file = directoryListing[0];
            try {
                if(outputFile.toFile().exists() && outputFile.toFile().isFile()) {
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

    private static GoGoDuckModule getProfileModule(String profile, String geoserver, String subset, UserLog userLog) {
        String thisPackage = GoGoDuckModule.class.getPackage().getName();
        String classToInstantiate = String.format("GoGoDuckModule_%s", profile);

        GoGoDuckModule module = null;
        while (null == module && "" != classToInstantiate) {
            logger.debug(String.format("Trying class '%s.%s'", thisPackage, classToInstantiate));
            try {
                Class classz = Class.forName(String.format("%s.%s", thisPackage, classToInstantiate));
                module = (GoGoDuckModule) classz.newInstance();
                module.init(profile, geoserver, subset, userLog);
                logger.info(String.format("Using class '%s.%s'", thisPackage, classToInstantiate));
                return module;
            }
            catch (Exception e) {
                logger.debug(String.format("Could not find class for '%s.%s'", thisPackage, classToInstantiate));
            }
            classToInstantiate = nextProfile(classToInstantiate);
        }

        throw new GoGoDuckException(String.format("Error initializing class for profile '%s'", profile));
    }

    /* Finds the correct profile to run for the given layer, starts with:
       GoGoDuckModule_acorn_hourly_avg_sag_nonqc_timeseries_url
       GoGoDuckModule_acorn_hourly_avg_sag_nonqc_timeseries
       GoGoDuckModule_acorn_hourly_avg_sag_nonqc
       GoGoDuckModule_acorn_hourly_avg_sag
       GoGoDuckModule_acorn_hourly_avg
       GoGoDuckModule_acorn_hourly
       GoGoDuckModule_acorn
       GoGoDuckModule */
    private static String nextProfile(String profile) {
        return profile.substring(0, profile.lastIndexOf("_"));
    }

    public static int execute(List<String> command) throws Exception {
        logger.info(command.toString());

        ProcessBuilder builder = new ProcessBuilder(command);
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

    public class UserLog {
        private FileWriter userLogFileWriter;
        UserLog() {
            this.userLogFileWriter = null;
        }

        UserLog(String logFile) {
            try {
                this.userLogFileWriter = new FileWriter(logFile);
            }
            catch (IOException e) {
                this.userLogFileWriter = null;
                logger.info(String.format("Exception while opening user log at '%s', continuing anyway", logFile));
            }
        }

        public void close() {
            try {
                if (null != userLogFileWriter) {
                    userLogFileWriter.close();

                }
            }
            catch (IOException e) {
                logger.debug("Could not close user log file");
            }
        }

        public void log(String s) {
            if (null != userLogFileWriter) {
                try {
                    userLogFileWriter.write(s);
                    userLogFileWriter.write(System.lineSeparator());
                }
                catch (IOException e) {
                    logger.debug("Exception while writing to user log");
                }
            }
        }
    }
}
