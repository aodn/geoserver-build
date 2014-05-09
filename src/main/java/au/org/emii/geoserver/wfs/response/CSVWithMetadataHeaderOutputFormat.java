/*
 * Copyright 2013 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */
package au.org.emii.geoserver.wfs.response;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.response.CSVOutputFormat;
import org.geotools.data.FeatureSource;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.jdbc.JDBCDataStore;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.FeatureType;

public class CSVWithMetadataHeaderOutputFormat extends WFSGetFeatureOutputFormat {

    static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("au.org.emii.geoserver.wfs.response");

    private Catalog catalog;
    private CSVOutputFormat csvOutputFormat;

    public CSVWithMetadataHeaderOutputFormat(GeoServer gs, Catalog catalog, CSVOutputFormat csvOutputFormat) {
        super(gs, "csv-with-metadata-header");
        this.catalog = catalog;
        this.csvOutputFormat = csvOutputFormat;
    }

    @Override
    public String getMimeType(Object value, Operation operation) {
        return this.csvOutputFormat.getMimeType(value, operation);
    }

    @Override
    public String getPreferredDisposition(Object value, Operation operation) {
        return this.csvOutputFormat.getPreferredDisposition(value, operation);
    }

    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        return this.csvOutputFormat.getAttachmentFileName(value, operation);
    }

    @Override
    public String getCapabilitiesElementName() {
        return this.csvOutputFormat.getCapabilitiesElementName();
    }

    @Override
    protected void write(FeatureCollectionResponse featureCollection,
                         OutputStream output,
                         Operation getFeature) throws IOException, ServiceException {
        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(output));
        writeMetadata(featureCollection, getMetadataFeatureName(featureCollection), w);
        this.csvOutputFormat.write(featureCollection, output, getFeature);
    }

    private void writeMetadata(FeatureCollectionResponse featureCollection,
                               String metadataFeatureName,
                               BufferedWriter w) throws IOException {

        String metadataSummaryBuildFunction = "build_metadata_summary";

        JDBCDataStore dataStore = getDataStoreForFeatureCollection(featureCollection);
        Connection cx = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            cx = dataStore.getConnection(Transaction.AUTO_COMMIT);
            stmt = cx.prepareStatement("select * from " + metadataFeatureName);
            rs = stmt.executeQuery();

            while (rs.next()) {
                w.write(rs.getString(1));
                w.newLine();
            }

            w.newLine();
            w.flush();
        }
        catch (SQLException e) {
            LOGGER.warning(e.getMessage());
        }
        finally {
            closeSafe(dataStore, cx, stmt, rs);
        }
    }

    private JDBCDataStore getDataStoreForFeatureCollection(
        FeatureCollectionResponse featureCollection) throws IOException {

        SimpleFeatureCollection fc = (SimpleFeatureCollection) featureCollection.getFeature().get(0);
        String typeName = fc.getSchema().getName().toString();
        FeatureTypeInfo fi = catalog.getFeatureTypeByName(typeName);
        DataStoreInfo dsi = fi.getStore();
        return (JDBCDataStore)dsi.getDataStore(null);
    }

    private String getMetadataFeatureName(
            FeatureCollectionResponse featureCollection) throws IOException {
        SimpleFeatureCollection fc = (SimpleFeatureCollection) featureCollection.getFeature().get(0);

        // There is an assumption that metadata will be in a table/view under the "parameters" schema, with
        // the naming scheme:
        //
        //     <featureCollection schema>_metadata_summary
        //
        String schema = getDataStoreForFeatureCollection(featureCollection).getDatabaseSchema();
        return "parameters." + schema + "_metadata_summary";
    }

    private void closeSafe(JDBCDataStore dataStore, Connection con, Statement stmt, ResultSet rs) {
        dataStore.closeSafe(rs);
        dataStore.closeSafe(stmt);
        dataStore.closeSafe(con);
    }
}
