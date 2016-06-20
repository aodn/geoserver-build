package au.org.emii.wps;

import au.org.emii.gogoduck.worker.*;
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

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.dom4j.tree.DefaultElement;
import org.dom4j.xpath.DefaultXPath;
import java.io.File;
import java.nio.file.Path;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.*;

@DescribeProcess(title="GoGoDuck", description="Subset and download gridded collection as NetCDF files")
public class GoGoDuckProcess extends AbstractNotifierProcess {
    static final Logger logger = LoggerFactory.getLogger(GoGoDuckProcess.class);
    private final Catalog catalog;
    private final GeoServerResourceLoader resourceLoader;

    public GoGoDuckProcess(WPSResourceManager resourceManager, HttpNotifier httpNotifier,
            Catalog catalog, GeoServerResourceLoader resourceLoader, GeoServer geoserver) {
        super(resourceManager, httpNotifier, geoserver);
        this.catalog = catalog;
        this.resourceLoader = resourceLoader;
        URLMangler.setUrlManglingMap(getConfigMap("/gogoduck/urlSubstitution"));
    }

    @DescribeResult(name="result", description="Aggregation result file", meta={"mimeTypes=application/x-netcdf"})
    public FileRawData execute(
            @DescribeParameter(name="layer", description="WFS layer to query")
            String layer,
            @DescribeParameter(name="subset", description="Subset, semi-colon separated")
            String subset,
            @DescribeParameter(name="filter", description="Post-processing filter to apply on output file", min=0)
            String filter,
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

            List<Converter> converters = GoGoDuckUtils.addFilters(filter);

            File outputFile = getResourceManager().getOutputResource(
                    getResourceManager().getExecutionId(true), layer + ".nc").file();

            String filePath = outputFile.toPath().toAbsolutePath().toString();

            GoGoDuck ggd = new GoGoDuck(catalog, layer, subset, filePath, converters, fileLimit);

            ggd.setTmpDir(getWorkingDir());
            ggd.setThreadCount(threadCount);
            ggd.setProgressListener(progressListener);

            Path outputPath = ggd.run();
            notifySuccess(callbackUrl, callbackParams);
            return new FileRawData(outputPath.toFile(), ggd.getMimeType(), ggd.getExtension());
        } catch (GoGoDuckException e) {
            logger.error(e.toString());
            notifyFailure(callbackUrl, callbackParams);
            throw new ProcessException(e.getMessage());
        }
    }

    private int getFileLimit() {
        return Integer.parseInt(getConfigVariable(GoGoDuckConfig.FILE_LIMIT_KEY, GoGoDuckConfig.FILE_LIMIT_DEFAULT));
    }

    private int getThreadCount() {
        return Integer.parseInt(getConfigVariable(GoGoDuckConfig.THREAD_COUNT_KEY, GoGoDuckConfig.THREAD_COUNT_DEFAULT));
    }

    private String getConfigFile() {
        return FilenameUtils.concat(resourceLoader.getBaseDirectory().toString(), GoGoDuckConfig.CONFIG_FILE);
    }

    public String getConfigVariable(String xpathString, String defaultValue) {
        SAXReader reader = new SAXReader();
        String returnValue = defaultValue;

        try {
            Document doc = reader.read(getConfigFile());
            DefaultXPath xpath = new DefaultXPath(xpathString);
            returnValue = xpath.selectSingleNode(doc).getText();
        }
        catch (DocumentException e) {
            logger.error(String.format("Could not open config file '%s': '%s'", getConfigFile(), e.getMessage()));
        }

        return returnValue;
    }

    public Map<String, String> getConfigMap(String xpathString) {
        SAXReader reader = new SAXReader();

        Map<String, String> returnValue = new HashMap<>();

        try {
            Document doc = reader.read(getConfigFile());
            DefaultXPath xpath = new DefaultXPath(xpathString);

            @SuppressWarnings("unchecked")
            List<DefaultElement> list = xpath.selectNodes(doc);

            for (final DefaultElement element : list) {
                returnValue.put(element.attribute("key").getText(), element.getText());
            }
        }
        catch (DocumentException e) {
            logger.error(String.format("Could not open config file '%s': '%s'", getConfigFile(), e.getMessage()));
        }

        return returnValue;
    }
}
