/*
 * Copyright 2013 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */
package au.org.emii.geoserver.wfs.response;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs.request.Query;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.type.DateUtil;
import org.geotools.jdbc.JDBCDataStore;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import javax.xml.namespace.QName;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class CSVWithMetadataHeaderOutputFormat extends WFSGetFeatureOutputFormat {

    static final Pattern CSV_ESCAPES = Pattern.compile("[\"\n,\r]");

    static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("au.org.emii.geoserver.wfs.response");

    private Catalog catalog;

    public CSVWithMetadataHeaderOutputFormat(GeoServer gs, Catalog catalog) {
        super(gs, "csv-with-metadata-header");
        this.catalog = catalog;
    }

    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return "text/csv";
    }

    public String getCapabilitiesElementName() {
        return "CSV";
    }

    @Override
    protected void write(FeatureCollectionResponse featureCollection,
                         OutputStream output,
                         Operation getFeature) throws IOException, ServiceException {
        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(output));
        writeMetadata(featureCollection, getMetadataFeatureName(featureCollection), w);
        writeData(featureCollection, output, getFeature);
    }


    public void writeData(FeatureCollectionResponse featureCollection, OutputStream output, Operation getFeature) throws IOException, ServiceException {
        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(output, this.gs.getGlobal().getSettings().getCharset()));
        FeatureCollection<?, ?> fc = (FeatureCollection)featureCollection.getFeature().get(0);
        Object att;

        SimpleFeatureType ft = (SimpleFeatureType)((FeatureCollection)fc).getSchema();
        w.write("FID,");

        for(int i = 0; i < ft.getAttributeCount(); ++i) {
            AttributeDescriptor ad = ft.getDescriptor(i);
            w.write(this.prepCSVField(ad.getLocalName()));
            if (i < ft.getAttributeCount() - 1) {
                w.write(",");
            }
        }

        w.write("\r\n");
        NumberFormat coordFormatter = NumberFormat.getInstance(Locale.US);
        coordFormatter.setMaximumFractionDigits(this.getInfo().getGeoServer().getSettings().getNumDecimals());
        coordFormatter.setGroupingUsed(false);
        FeatureIterator i = ((FeatureCollection)fc).features();

        try {
            while (i.hasNext()) {
                Feature f = i.next();
                w.write(this.prepCSVField(f.getIdentifier().getID()));
                w.write(",");

                for(int j = 0; j < ((SimpleFeature)f).getAttributeCount(); ++j) {
                    att = ((SimpleFeature)f).getAttribute(j);
                    if (att != null) {
                        String value = formatToString(att, coordFormatter);
                        w.write(this.prepCSVField(value));
                    }

                    if (j < ((SimpleFeature)f).getAttributeCount() - 1) {
                        w.write(",");
                    }
                }
                w.write("\r\n");
            }
        } finally {
            i.close();
        }
        w.flush();
    }

    private String prepCSVField(String field) {
        String mod = field.replaceAll("\"", "\"\"");
        if (CSV_ESCAPES.matcher(mod).find()) {
            mod = "\"" + mod + "\"";
        }
        return mod;
    }

    public String formatToString(Object att, NumberFormat coordFormatter) {
        String value = null;

        if (att instanceof Number) {
            value = coordFormatter.format(att);
        } else if (att instanceof Date) {
            if (att instanceof java.sql.Date) {
                value = DateUtil.serializeSqlDate((java.sql.Date)att);
            } else if (att instanceof Time) {
                value = DateUtil.serializeSqlTime((Time)att);
            }else {
                value = utcDateString((Date)att);
            }
        } else {
            value = att.toString();
        }
        return value;
    }

    private String utcDateString(Date date) {
        DateTime dt = new DateTime( date, DateTimeZone.UTC );

        DateTimeFormatter fmt = ISODateTimeFormat.dateTimeNoMillis();
        return fmt.print(dt);
    }

    private void writeMetadata(FeatureCollectionResponse featureCollection,
                               String metadataFeatureName,
                               BufferedWriter w) throws IOException {

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
        // There is an assumption that metadata will be in a table/view under the "parameters_mapping" schema, with
        // the naming scheme:
        //
        //     <featureCollection schema>_metadata_summary
        //
        String schema = getDataStoreForFeatureCollection(featureCollection).getDatabaseSchema();

        return "parameters_mapping." + schema + "_metadata_summary";
    }

    private void closeSafe(JDBCDataStore dataStore, Connection con, Statement stmt, ResultSet rs) {
        dataStore.closeSafe(rs);
        dataStore.closeSafe(stmt);
        dataStore.closeSafe(con);
    }

    public String getAttachmentFileName(Object value, Operation operation) {
        GetFeatureRequest request = GetFeatureRequest.adapt(operation.getParameters()[0]);
        String outputFileName = ((QName)((Query)request.getQueries().get(0)).getTypeNames().get(0)).getLocalPart();
        return outputFileName + ".csv";
    }
}
