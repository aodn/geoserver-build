package au.org.emii.wps;

import au.org.emii.aggregator.NetcdfAggregator;
import au.org.emii.download.Download;
import au.org.emii.download.DownloadConfig;
import au.org.emii.download.DownloadRequest;
import au.org.emii.download.Downloader;
import au.org.emii.download.ParallelDownloadManager;
import au.org.emii.notifier.HttpNotifier;
import au.org.emii.wps.catalogue.CatalogueReader;
import au.org.emii.wps.gogoduck.config.GoGoDuckConfig;
import au.org.emii.wps.gogoduck.converter.Converter;
import au.org.emii.wps.gogoduck.exception.GoGoDuckException;
import au.org.emii.wps.gogoduck.index.FeatureSourceIndexReader;
import au.org.emii.wps.gogoduck.index.IndexReader;
import au.org.emii.wps.gogoduck.parameter.SubsetParameters;
import au.org.emii.wps.process.StringRawData;
import au.org.emii.wps.provenance.ProvenanceWriter;
import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.wps.process.FileRawData;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.factory.DescribeResults;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.opengis.util.ProgressListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@DescribeProcess(title="GoGoDuck", description="Subset and download gridded collection as NetCDF files")
public class GoGoDuckProcess extends AbstractNotifierProcess {
    private static final Logger logger = LoggerFactory.getLogger(GoGoDuckProcess.class);
    private final Catalog catalog;
    private final ProvenanceWriter provenanceWriter;
    private GoGoDuckConfig config;

    public GoGoDuckProcess(WPSResourceManager resourceManager, ProvenanceWriter provenanceWriter, HttpNotifier httpNotifier,
                           Catalog catalog, GeoServerResourceLoader resourceLoader, GeoServer geoserver,
                           CatalogueReader metadataCatalogue) {
        super(resourceManager, httpNotifier, geoserver, metadataCatalogue);
        this.catalog = catalog;
        this.config = new GoGoDuckConfig(resourceLoader.getBaseDirectory(), catalog);
        this.provenanceWriter = provenanceWriter;
    }

    @DescribeResults({
            @DescribeResult(description="Aggregation result file", meta={"mimeTypes=application/x-netcdf,text/csv",
                    "chosenMimeType=format"}, type=FileRawData.class),
            @DescribeResult(name="provenance", description="Provenance document", meta={"mimeTypes=application/xml"}, type=StringRawData.class)
    })

    public Map<String,Object> execute(
            @DescribeParameter(name="layer", description="WFS layer to query")
            String layer,
            @DescribeParameter(name="subset", description="Subset, semi-colon separated. Example: TIME,2009-01-01T00:00:00.000Z,2009-12-25T23:04:00.000Z;LATITUDE,-33.433849,-32.150743;LONGITUDE,114.15197,115.741219")
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
            Path convertedFile;
            String mimeType;
            String extension;
            String provenanceDocument;

            try {
                DateTime startTime = new DateTime(DateTimeZone.UTC);

                SubsetParameters parameters = SubsetParameters.parse(subset);

                IndexReader indexReader = new FeatureSourceIndexReader(catalog, config.getUrlSubstitution(layer));

                Set<DownloadRequest> downloads = indexReader.getDownloadList(layer, config.getTimeField(),
                    config.getSizeField(), config.getFileUrlField(), parameters);

                enforceFileLimits(parameters, downloads, config.getFileLimit(), config.getFileSizeLimit());

                Path workingDir = Paths.get(getWorkingDir());
                Path downloadDir = workingDir.resolve("downloads");
                Files.createDirectories(downloadDir);

                DownloadConfig downloadConfig = new DownloadConfig.ConfigBuilder()
                    .downloadDirectory(downloadDir)
                    .localStorageLimit(config.getStorageLimit())
                    .poolSize(config.getThreadCount())
                    .build();

                Downloader downloader = new Downloader(config.getConnectTimeOut(), config.getReadTimeOut());

                Path outputFile = workingDir.resolve("aggregation.nc");

                try (
                    ParallelDownloadManager downloadManager = new ParallelDownloadManager(downloadConfig, downloader);
                    NetcdfAggregator aggregator = new NetcdfAggregator(
                        outputFile, config.getTemplate(layer), config.getMaxChunkSize(),
                        parameters.getBbox(), parameters.getVerticalRange(), parameters.getTimeRange()
                    )
                ){
                    for (Download download : downloadManager.download(downloads)) {
                        aggregator.add(download.getPath());
                        downloadManager.remove();
                        throwIfCancelled(progressListener);
                    }
                }

                Converter converter = Converter.newInstance(format);
                convertedFile = workingDir.resolve("converted" + converter.getExtension());
                converter.convert(outputFile, convertedFile);
                mimeType = converter.getMimeType();
                extension = converter.getExtension();
                throwIfCancelled(progressListener);

                // Create provenance document
                Map<String, Object> params = new HashMap<>();
                params.put("jobId", getId());
                params.put("downloadUrl", getOutputResourceUrl("result", extension, mimeType));
                params.put("settingsPath", config.getTemplatePath(layer));
                params.put("startTime", startTime);
                params.put("endTime", new DateTime(DateTimeZone.UTC));
                params.put("layer", layer);
                params.put("parameters", parameters);
                params.put("sourceMetadataUrl", getMetadataUrl(layer));
                provenanceDocument = provenanceWriter.write("provenance_template_gridded.ftl", params);
            }
            catch (Exception e) {
                String errMsg = String.format("Your aggregation failed! Reason for failure is: '%s'", e.getMessage());
                logger.error(errMsg, e);
                throw new GoGoDuckException(e.getMessage(), e);
            }

            Map result = new HashMap();

            result.put("result", new  FileRawData(convertedFile.toFile(), mimeType, extension));
            result.put("provenance", new StringRawData(provenanceDocument, "application/xml", "xml"));

            notifySuccess(callbackUrl, callbackParams);

            return result;

        } catch (GoGoDuckException e) {
            logger.error(e.toString(), e);
            notifyFailure(callbackUrl, callbackParams);
            throw new ProcessException(e.getMessage());
        } catch (Exception e) {
            logger.error(e.toString(), e);
            notifyFailure(callbackUrl, callbackParams);
            throw new ProcessException("Failed to Subset/Download gridded collection(s)");
        }
    }

    private void enforceFileLimits(SubsetParameters parameters, Set<DownloadRequest> downloadList, Integer limit, double fileSizeLimit) throws GoGoDuckException {
        if (parameters.isPointSubset()) {
            logger.info("Not applying limits to point subset");
            return;
        }

        logger.info("Enforcing file size limits...");

        if (downloadList.size() > limit) {
            throw new GoGoDuckException(String.format("Aggregation asked for %d files, we allow only %d", downloadList.size(), limit));
        } else if (downloadList.size() == 0) {
            logger.error("No files returned for aggregation");
            throw new GoGoDuckException("No files returned from geoserver");
        }

        long totalSize = 0;

        for (DownloadRequest download: downloadList) {
            totalSize += download.getSize();
        }

        if (fileSizeLimit != 0.0 && totalSize != 0.0 && totalSize > fileSizeLimit) {
            String totalFileSize = FileUtils.byteCountToDisplaySize(totalSize);
            String sizeLimit = FileUtils.byteCountToDisplaySize((long)fileSizeLimit);
            throw new GoGoDuckException(String.format("Total file size %s for %s files, exceeds the limit %s", totalFileSize, downloadList.size(), sizeLimit));
        }
    }

    private synchronized boolean isCancelled(ProgressListener progressListener) {
        return null != progressListener && progressListener.isCanceled();
    }

    private void throwIfCancelled(ProgressListener progressListener) throws GoGoDuckException {
        if (isCancelled(progressListener))
            throw new GoGoDuckException("Job cancelled");
    }

}
