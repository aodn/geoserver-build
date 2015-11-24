package au.org.emii.wps;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.wps.ProcessDismissedException;
import org.geoserver.wps.process.FileRawData;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
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

import au.org.emii.ncdfgenerator.NcdfDefinition;
import au.org.emii.ncdfgenerator.NcdfDefinitionXMLParser;
import au.org.emii.ncdfgenerator.NcdfEncoder;
import au.org.emii.ncdfgenerator.NcdfEncoderBuilder;
import au.org.emii.ncdfgenerator.ZipFormatter;
import au.org.emii.notifier.HttpNotifier;

@DescribeProcess(title="NetCDF download", description="Subset and download collection as NetCDF files")
public class NetcdfOutputProcess extends AbstractNotifierProcess {

    private static final String NETCDF_FILENAME = "netcdf.xml";
    private static final Logger logger = LoggerFactory.getLogger(NetcdfOutputProcess.class);
    private final Catalog catalog;
    private final String workingDir;
    private final ServletContext context;

    public NetcdfOutputProcess(WPSResourceManager resourceManager, HttpNotifier httpNotifier,
            Catalog catalog, ServletContext context) {
        super(resourceManager, httpNotifier);
        this.catalog = catalog;
        this.context = context;
        this.workingDir = getWorkingDir();

        logger.info("constructor");
    }

    @DescribeResult(name="result", description="Zipped netcdf files", meta={"mimeTypes=application/zip"})

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

        Transaction transaction = null;
        Connection conn = null;
        InputStream config = null;
        FileOutputStream os = null;

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
            config = new FileInputStream(filePath);
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(config);
            Node node = document.getFirstChild();
            NcdfDefinition definition = new NcdfDefinitionXMLParser().parse(node);

            // get store
            String dataStoreName = definition.getDataSource().getDataStoreName();
            DataStoreInfo dsinfo = catalog.getDataStoreByName(dataStoreName);
            JDBCDataStore store = (JDBCDataStore)dsinfo.getDataStore(null);

            // resources
            transaction = new DefaultTransaction("handle");
            conn = store.getConnection(transaction);

            // create the netcdf encoder
            NcdfEncoderBuilder encoderBuilder = new NcdfEncoderBuilder();

            encoderBuilder.setTmpCreationDir(workingDir)
                .setDefinition(definition)
                .setFilterExpr(cqlFilter)
                .setDataStore(store)
                .setSchema(store.getDatabaseSchema())
            ;

            NcdfEncoder encoder = encoderBuilder.create();

            final File outputFile = getResourceManager().getOutputResource(
                getResourceManager().getExecutionId(true), typeName + ".zip").file();

            os = new FileOutputStream(outputFile);

            encoder.prepare(new ZipFormatter(os));
            while (encoder.writeNext()) {
                if (progressListener.isCanceled()) {
                    throw new ProcessDismissedException(progressListener);
                }
            }

            return new FileRawData(outputFile, "application/zip");

        } catch (Exception e) {
            if (transaction != null) {
                try {
                    transaction.close();
                } catch (IOException e_) {
                    logger.info("problem closing transaction");
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e_) {
                    logger.info("problem closing connection");
                }
            }
            throw new ProcessException(e.getMessage());
        } finally {
            IOUtils.closeQuietly(config);
            IOUtils.closeQuietly(os);

            notify(callbackUrl, callbackParams);
        }
    }

}
