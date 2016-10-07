package au.org.emii.wps;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
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

import au.org.emii.gogoduck.worker.GoGoDuck;
import au.org.emii.utils.GoGoDuckConfig;
import au.org.emii.gogoduck.exception.GoGoDuckException;
import au.org.emii.notifier.HttpNotifier;

@DescribeProcess(title="GoGoDuck", description="Subset and download gridded collection as NetCDF files")
public class GoGoDuckProcess extends AbstractNotifierProcess {
    static final Logger logger = LoggerFactory.getLogger(GoGoDuckProcess.class);
    private final Catalog catalog;
    private final GeoServerResourceLoader resourceLoader;
    private GoGoDuckConfig goGoDuckConfig;

    public GoGoDuckProcess(WPSResourceManager resourceManager, HttpNotifier httpNotifier,
            Catalog catalog, GeoServerResourceLoader resourceLoader, GeoServer geoserver) {
        super(resourceManager, httpNotifier, geoserver);
        this.catalog = catalog;
        this.resourceLoader = resourceLoader;
        this.goGoDuckConfig = new GoGoDuckConfig(resourceLoader.getBaseDirectory(), catalog);
    }

    @DescribeResult(name="result", description="Aggregation result file", meta={"mimeTypes=application/x-netcdf,text/csv",
            "chosenMimeType=format"})
    public FileRawData execute(
            @DescribeParameter(name="layer", description="WFS layer to query")
            String layer,
            @DescribeParameter(name="subset", description="Subset, semi-colon separated")
            String subset,
            @DescribeParameter(name="callbackUrl", description="Callback URL", min=0)
            URL callbackUrl,
            @DescribeParameter(name="callbackParams", description="Parameters to append to the callback", min=0)
            String callbackParams,
            @DescribeParameter(name = "format", min = 0)
            final String format,
            ProgressListener progressListener
    ) throws ProcessException {
        try {
            if (goGoDuckConfig.getThreadCount() <= 0) {
                throw new ProcessException("threadCount set to 0 or below, job will not run");
            }

            File outputFile;

            try {
                outputFile = getResourceManager().getTemporaryResource(".nc").file();
            } catch (IOException e) {
                throw new GoGoDuckException("Unable to obtain temporary file required for aggregation", e);
            }

            String filePath = outputFile.toPath().toAbsolutePath().toString();

            GoGoDuck ggd = new GoGoDuck(catalog, layer, subset, filePath, format, goGoDuckConfig);

            ggd.setTmpDir(getWorkingDir());
            ggd.setProgressListener(progressListener);

            Path outputPath = ggd.run();
            notifySuccess(callbackUrl, callbackParams);
            return new FileRawData(outputPath.toFile(), ggd.getMimeType(), ggd.getExtension());
        } catch (GoGoDuckException e) {
            logger.error(e.toString(), e);
            notifyFailure(callbackUrl, callbackParams);
            throw new ProcessException(e.getMessage(), e);
        }
    }
}
