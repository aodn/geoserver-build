package au.org.emii.wps;

import au.org.emii.ncdfgenerator.*;
import au.org.emii.notifier.HttpNotifier;
import au.org.emii.wps.provenance.ProvenanceWriter;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.wps.ProcessDismissedException;
import org.geoserver.wps.process.FileRawData;
import org.geoserver.wps.process.StringRawData;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.jdbc.JDBCDataStore;
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
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.servlet.ServletContext;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@DescribeProcess(title="NetCDF download", description="Subset and download collection as NetCDF files")
public class NetcdfOutputProcess extends AbstractNotifierProcess {

    private static final String NETCDF_FILENAME = "netcdf.xml";
    private static final Logger logger = LoggerFactory.getLogger(NetcdfOutputProcess.class);
    private final Catalog catalog;
    private final String workingDir;
    private final ServletContext context;
    private final ProvenanceWriter provenanceWriter;

    public NetcdfOutputProcess(WPSResourceManager resourceManager, ProvenanceWriter provenanceWriter, HttpNotifier httpNotifier,
            Catalog catalog, ServletContext context, GeoServer geoserver) {
        super(resourceManager, httpNotifier, geoserver);
        this.catalog = catalog;
        this.context = context;
        this.workingDir = getWorkingDir();
        this.provenanceWriter = provenanceWriter;
    }

    @DescribeResults({
            @DescribeResult(description = "Zipped netcdf files", meta = {"mimeTypes=application/zip"}, type=org.geoserver.wps.process.FileRawData.class),
            @DescribeResult(name = "provenance", description = "Provenance document", meta = {"mimeTypes=text/xml"}, type = org.geoserver.wps.process.StringRawData.class)
    })

    public Map<String,Object> execute(
        @DescribeParameter(name="typeName", description="Collection to download")
        String typeName,
        @DescribeParameter(name="cqlFilter", description="CQL Filter to apply", min=0)
        String cqlFilter,
        @DescribeParameter(name="callbackUrl", description="Callback URL", min=0)
        URL callbackUrl,
        @DescribeParameter(name="callbackParams", description="Parameters to append to the callback", min=0)
        String callbackParams,
        ProgressListener progressListener
    ) throws ProcessException {

        logger.info("execute");
        String provenanceDocument;

        try {
            // lookup the layer in the catalog
            LayerInfo layerInfo = catalog.getLayerByName(typeName);

            if (layerInfo == null) {
                throw new ProcessException(String.format("Failed to find typename '%s'", typeName));
            }

            // get xml definition file path
            String dataPath = GeoServerResourceLoader.lookupGeoServerDataDirectory(context);
            GeoServerDataDirectory dataDirectory = new GeoServerDataDirectory(new File(dataPath));
            String typeNamePath = dataDirectory.get(layerInfo).dir().getAbsolutePath();
            String filePath = typeNamePath + "/" + NETCDF_FILENAME;
            String settingsFile = dataDirectory.get(layerInfo).dir() + "/" + NETCDF_FILENAME;

            // decode definition
            NcdfDefinition definition = decodeDefinition(filePath);

            // get store
            String dataStoreName = definition.getDataSource().getDataStoreName();
            DataStoreInfo dsinfo = catalog.getDataStoreByName(dataStoreName);
            JDBCDataStore store = (JDBCDataStore) dsinfo.getDataStore(null);

            // create the netcdf encoder
            NcdfEncoderBuilder encoderBuilder = new NcdfEncoderBuilder();

            encoderBuilder.setTmpCreationDir(workingDir)
                .setDefinition(definition)
                .setFilterExpr(cqlFilter)
                .setDataStore(store)
                .setSchema(store.getDatabaseSchema());

            final File outputFile = getResourceManager().getTemporaryResource(".zip").file();
            String aggregatedDataUrl = String.format("%s?service=WPS&amp;version=1.0.0&amp;request=GetExecutionResult&amp;executionId=%s&amp;outputId=result.zip&amp;mimetype=application%%2Fzip",
                    getWpsUrl().toString(), getId());

            try (
                    NcdfEncoder encoder = encoderBuilder.create();
                    FileOutputStream os = new FileOutputStream(outputFile))
            {
                // Create provenance document
                Map<String, Object> params = new HashMap<>();
                params.put("jobId", getId());
                params.put("aggregatedDataUrl", aggregatedDataUrl);
                params.put("outputAggregationSettingsXml", settingsFile);
                params.put("wpsQuery", cqlFilter);
                params.put("startTime", new DateTime(DateTimeZone.UTC));
                params.put("endTime", "");
/*                params.put("temporalStart", parameters.getTimeRange().getStart());
                params.put("temporalEnd", parameters.getTimeRange().getEnd());
                params.put("westBL", parameters.getBbox().getLonMin());
                params.put("eastBL", parameters.getBbox().getLonMax());
                params.put("northBL", parameters.getBbox().getLatMax());
                params.put("southBL", parameters.getBbox().getLatMin());*/
                params.put("layerName", layerInfo.getResource().getName());
                params.put("sourceMetadataUrl", getMetadataUrl(layerInfo.getName()));
                params.put("creationTime",  new DateTime(DateTimeZone.UTC));
                provenanceDocument = provenanceWriter.write("provenance_template_non_gridded.ftl", params);

                encoder.prepare(new ZipFormatter(os));
                while (encoder.writeNext()) {
                    if (progressListener.isCanceled()) {
                        throw new ProcessDismissedException("The job has been cancelled");
                    }
                }
            }

            notifySuccess(callbackUrl, callbackParams);

            Map result = new HashMap();
            result.put("result", new  FileRawData(outputFile, "application/zip", "zip"));
            result.put("provenance", new StringRawData(provenanceDocument, "text/xml"));

            return result;

        } catch (Exception e) {
            notifyFailure(callbackUrl, callbackParams);
            throw new ProcessException(e);
        }
    }

    private NcdfDefinition decodeDefinition(String filePath) throws Exception {
        try (InputStream config = new FileInputStream(filePath)) {
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(config);
            Node node = document.getFirstChild();
            return new NcdfDefinitionXMLParser().parse(node);
        }
    }
}
