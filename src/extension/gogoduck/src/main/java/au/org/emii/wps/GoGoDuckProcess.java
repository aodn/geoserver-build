package au.org.emii.wps;

import java.io.File;

import au.org.emii.gogoduck.worker.GoGoDuckException;
import au.org.emii.gogoduck.worker.GoGoDuck;

import net.opengis.wps10.ExecuteType;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.ows.Dispatcher;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.Operation;
import org.geoserver.platform.exception.GeoServerRuntimException;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.WPSInfo;
import org.opengis.util.ProgressListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geoserver.wps.gs.GeoServerProcess;
import org.geoserver.wps.process.FileRawData;
import org.geoserver.wps.resource.WPSResourceManager;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

@DescribeProcess(title="GoGoDuck", description="Subset and download gridded collection as NetCDF files")
public class GoGoDuckProcess implements GeoServerProcess {
    private static final Logger logger = LoggerFactory.getLogger(GoGoDuck.class);

    private final WPSResourceManager resourceManager;
    private final WPSInfo wpsInfo;
    private final GeoServerResourceLoader resourceLoader;

    private final String CONFIG_FILE = "wps/gogoduck.xml";

    private final String FILE_LIMIT_KEY = "/gogoduck/fileLimit";
    private final String FILE_LIMIT_DEFAULT = "10";

    private final String THREAD_COUNT_KEY = "/gogoduck/threadCount";
    private final String THREAD_COUNT_DEFAULT = "0";

    public GoGoDuckProcess(WPSResourceManager resourceManager, GeoServerImpl geoServer, GeoServerResourceLoader resourceLoader) {
        this.resourceManager = resourceManager;
        this.resourceLoader = resourceLoader;
        wpsInfo = geoServer.getService(WPSInfo.class);
    }

    @DescribeResult(name="result", description="NetCDF file", meta={"mimeTypes=application/x-netcdf"})
    public FileRawData execute(
            @DescribeParameter(name="layer", description="WFS layer to query")
            String layer,
            @DescribeParameter(name="subset", description="Subset, semi-colon separated")
            String subset,
            ProgressListener progressListener
    ) throws ProcessException {
        try {
            final int threadCount = getThreadCount();
            final int fileLimit = getFileLimit();

            if (threadCount <= 0) {
                throw new ProcessException("threadCount set to 0 or below, job will not run");
            }

            if (fileLimit <= 0) {
                throw new ProcessException("fileLimit set to 0 or below, job will not run");
            }

            final File outputFile = resourceManager.getOutputResource(
                    resourceManager.getExecutionId(true), layer + ".nc").file();

            final String filePath = outputFile.toPath().toAbsolutePath().toString();

            GoGoDuck ggd = new GoGoDuck(getBaseUrl(), layer, subset, filePath, fileLimit);

            ggd.setTmpDir(getWorkingDir(resourceManager));
            ggd.setThreadCount(threadCount);
            ggd.setProgressListener(progressListener);

            ggd.run();
            return new FileRawData(outputFile, "application/x-netcdf", "nc");
        } catch (GoGoDuckException e) {
            logger.error(e.toString());
            throw new ProcessException(e);
        }
    }

    private String getBaseUrl() {
        // TODO is there a nicer way of getting BaseUrl?
        Dispatcher.REQUEST.get().getOperation();
        Operation op = Dispatcher.REQUEST.get().getOperation();
        ExecuteType execute = (ExecuteType) op.getParameters()[0];
        return execute.getBaseUrl();
    }

    private int getFileLimit() {
        return Integer.parseInt(getConfigVariable(FILE_LIMIT_KEY, FILE_LIMIT_DEFAULT));
    }

    private int getThreadCount() {
        return Integer.parseInt(getConfigVariable(THREAD_COUNT_KEY, THREAD_COUNT_DEFAULT));
    }

    private String getConfigVariable(String key, String defaultValue) {
        String returnValue = defaultValue;
        try {
            File configFile = new File(FilenameUtils.concat(resourceLoader.getBaseDirectory().toString(), CONFIG_FILE));
            logger.debug(String.format("Config file is at '%s'", configFile.toString()));

            returnValue = getXPathValue(configFile, key);
            logger.debug(String.format("Loaded config value '%s' -> '%s'", key, returnValue));
        }
        catch (Exception e) {
            logger.error(String.format("Could not open config file '%s': '%s'", CONFIG_FILE, e.getMessage()));
        }

        return returnValue;
    }

    private String getXPathValue(File xmlFile, String xpath) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlFile);
        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xp = xpathFactory.newXPath();

        XPathExpression expr = xp.compile(String.format("%s/text()", xpath));
        return (String) expr.evaluate(doc, XPathConstants.STRING);
    }

    private String getWorkingDir(WPSResourceManager resourceManager) {
        try {
            // Use WPSResourceManager to create a temporary directory that will get cleaned up
            // when the process has finished executing (Hack! Should be a method on the resource manager)
            return resourceManager.getTemporaryResource("").dir().getAbsolutePath();
        } catch (Exception e) {
            logger.info("Exception accessing working directory: \n" + e);
            return System.getProperty("java.io.tmpdir");
        }
    }
}
