package au.org.emii.wps;

import au.org.emii.ncdfgenerator.*;
import au.org.emii.notifier.HttpNotifier;
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
import org.opengis.util.ProgressListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.servlet.ServletContext;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.URL;

@DescribeProcess(title="NetCDF download", description="Subset and download collection as NetCDF files")
public class NetcdfOutputProcess extends AbstractNotifierProcess {

    private static final String NETCDF_FILENAME = "netcdf.xml";
    private static final Logger logger = LoggerFactory.getLogger(NetcdfOutputProcess.class);
    private final Catalog catalog;
    private final String workingDir;
    private final ServletContext context;

    public NetcdfOutputProcess(WPSResourceManager resourceManager, HttpNotifier httpNotifier,
            Catalog catalog, ServletContext context, GeoServer geoserver) {
        super(resourceManager, httpNotifier, geoserver);
        this.catalog = catalog;
        this.context = context;
        this.workingDir = getWorkingDir();
    }

    @DescribeResult(description="Zipped netcdf files", meta={"mimeTypes=application/zip"})

    public FileRawData execute(
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
                encoder.prepare(new ZipFormatter(os));
                while (encoder.writeNext()) {
                    if (progressListener.isCanceled()) {
                        throw new ProcessDismissedException("The job has been cancelled");
                    }
                }
            }

            notifySuccess(callbackUrl, callbackParams);

            return new FileRawData(outputFile, "application/zip", "zip");

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
