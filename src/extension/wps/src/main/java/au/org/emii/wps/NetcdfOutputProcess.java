package au.org.emii.wps;

import au.org.emii.ncdfgenerator.*;
import au.org.emii.notifier.HttpNotifier;
import au.org.emii.wps.catalogue.CatalogueReader;
import au.org.emii.wps.process.StringRawData;
import au.org.emii.wps.provenance.ProvenanceWriter;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.wps.ProcessDismissedException;
import org.geoserver.wps.process.FileRawData;
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
                               Catalog catalog, ServletContext context, GeoServer geoserver, CatalogueReader metadataCatalogue) {
        super(resourceManager, httpNotifier, geoserver, metadataCatalogue);
        this.catalog = catalog;
        this.context = context;
        this.workingDir = getWorkingDir();
        this.provenanceWriter = provenanceWriter;
    }

    @DescribeResults({
            @DescribeResult(description = "Zipped netcdf files", meta = {"mimeTypes=application/zip"}, type=FileRawData.class),
            @DescribeResult(name = "provenance", description = "Provenance document", meta = {"mimeTypes=application/xml"}, type = StringRawData.class)
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

        DateTime startTime = new DateTime(DateTimeZone.UTC);

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
            String settingsPath = dataDirectory.get(layerInfo).path() + "/" + NETCDF_FILENAME;

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

            try (
                    NcdfEncoder encoder = encoderBuilder.create();
                    FileOutputStream os = new FileOutputStream(outputFile))
            {
                // Create provenance document
                Map<String, Object> params = new HashMap<>();
                params.put("jobId", getId());
                params.put("aggregatedDataUrl", getOutputResourceUrl("result", "zip", "application/zip"));
                params.put("settingsPath", settingsPath);
                params.put("wpsQuery", cqlFilter);
                params.put("startTime", startTime);
                params.put("endTime", new DateTime(DateTimeZone.UTC));
                params.put("layerName", layerInfo.getResource().getName());
                params.put("sourceMetadataUrl", getMetadataUrl(layerInfo.getName()));
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
            result.put("provenance", new StringRawData(provenanceDocument, "application/xml", "xml"));

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
