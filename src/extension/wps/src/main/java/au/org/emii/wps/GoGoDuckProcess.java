package au.org.emii.wps;

import au.org.emii.gogoduck.worker.GoGoDuck;
import au.org.emii.gogoduck.worker.GoGoDuckException;
import au.org.emii.gogoduck.worker.URLMangler;
import au.org.emii.notifier.HttpNotifier;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.config.GeoServer;
import org.geoserver.catalog.Catalog;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.wps.process.FileRawData;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.opengis.util.ProgressListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.net.URL;
import java.util.Map;

@DescribeProcess(title="GoGoDuck", description="Subset and download gridded collection as NetCDF files")
public class GoGoDuckProcess extends AbstractNotifierProcess {
    static final Logger logger = LoggerFactory.getLogger(GoGoDuck.class);

    private final Catalog catalog;
    private final GeoServerResourceLoader resourceLoader;

    private final String CONFIG_FILE = "wps/gogoduck.xml";

    private final String FILE_LIMIT_KEY = "/gogoduck/fileLimit";
    private final String FILE_LIMIT_DEFAULT = "10";

    private final String THREAD_COUNT_KEY = "/gogoduck/threadCount";
    private final String THREAD_COUNT_DEFAULT = "0";

    public GoGoDuckProcess(WPSResourceManager resourceManager, HttpNotifier httpNotifier,
            Catalog catalog, GeoServerResourceLoader resourceLoader, GeoServer geoserver) {
        super(resourceManager, httpNotifier, geoserver);
        this.catalog = catalog;
        this.resourceLoader = resourceLoader;
    }

    @DescribeResult(name="result", description="NetCDF file", meta={"mimeTypes=application/x-netcdf"})
    public FileRawData execute(
            @DescribeParameter(name="layer", description="WFS layer to query")
            String layer,
            @DescribeParameter(name="subset", description="Subset, semi-colon separated")
            String subset,
            @DescribeParameter(name="callbackUrl", description="Callback URL", min=0)
            URL callbackUrl,
            @DescribeParameter(name="callbackParams", description="Parameters to append to the callback", min=0)
            String callbackParams,
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

            final File outputFile = getResourceManager().getOutputResource(
                    getResourceManager().getExecutionId(true), layer + ".nc").file();

            final String filePath = outputFile.toPath().toAbsolutePath().toString();

            GoGoDuck ggd = new GoGoDuck(catalog, layer, subset, filePath, fileLimit);

            ggd.setTmpDir(getWorkingDir());
            ggd.setThreadCount(threadCount);
            ggd.setProgressListener(progressListener);

            ggd.run();
            notifySuccess(callbackUrl, callbackParams);
            return new FileRawData(outputFile, "application/x-netcdf", "nc");
        } catch (GoGoDuckException e) {
            logger.error(e.toString());
            notifyFailure(callbackUrl, callbackParams);
            throw new ProcessException(e);
        }
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

    public void setUrlMangling(Map<String, String> urlMangling) {
        URLMangler.setUrlManglingMap(urlMangling);
    }

}
