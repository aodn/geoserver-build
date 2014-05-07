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
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
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

        /**
         * Assumes a function 'build_metadata_summary' exists, e.g.:
         *
         *   CREATE OR REPLACE FUNCTION build_metadata_summary(feature_name TEXT) RETURNS TEXT AS $$
         *     BEGIN
         *       RETURN 'The wonderful metadata summary for ' || feature_name;
         *     END;
         *   $$ LANGUAGE plpgsql;
         */
        String metadataSummaryBuildFunction = "build_metadata_summary";

        try {
            Connection cx = getConnectionForFeatureCollection(featureCollection);
            CallableStatement stmt = cx.prepareCall("{call " + metadataSummaryBuildFunction + "(?)}");

            stmt.setString(1, metadataFeatureName);
            ResultSet rs = stmt.executeQuery();

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
    }

    private Connection getConnectionForFeatureCollection(FeatureCollectionResponse featureCollection) throws IOException {
        SimpleFeatureCollection fc = (SimpleFeatureCollection) featureCollection.getFeature().get(0);
        String typeName = fc.getSchema().getName().toString();
        FeatureTypeInfo fi = catalog.getFeatureTypeByName(typeName);
        DataStoreInfo dsi = fi.getStore();
        JDBCDataStore jds = (JDBCDataStore)dsi.getDataStore(null);

        return jds.getConnection(Transaction.AUTO_COMMIT);
    }

    private String getMetadataFeatureName(
            FeatureCollectionResponse featureCollection) {
        SimpleFeatureCollection fc = (SimpleFeatureCollection) featureCollection.getFeature().get(0);

        // There is an assumption that WFS features are conventionally suffixed with "_data".
        return fc.getSchema().getName().getLocalPart().replace("_data", "");
    }

    @Override
    public String getCapabilitiesElementName() {
        return this.csvOutputFormat.getCapabilitiesElementName();
    }
}
