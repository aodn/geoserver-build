package au.org.emii.geoserver.wfs.response;

import au.org.emii.geoserver.extensions.filters.layer.data.DataDirectory;
import au.org.emii.geoserver.wfs.response.config.PivotConfig;
import au.org.emii.geoserver.wfs.response.config.PivotConfigFile;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.platform.Operation;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.jdbc.JDBCDataStore;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static au.org.emii.geoserver.wfs.response.CSVWithMetadataHeaderOutputFormat.JSON_FIELD_TYPE;


public class CsvPivotedFeatureCollectionSource implements CsvSource {

    private final SimpleFeatureIterator featureIterator;
    private final ArrayList<String> nonPivotColumnNames;
    private final List<String> pivotColumnNames;
    private final List<String> pivotSourceColumnNames;
    private final PivotConfig pivotConfig;

    static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("au.org.emii.geoserver.wfs.response");

    private SimpleFeature nextFeature;
    private final Catalog catalog;
    private final ServletContext context;

    public CsvPivotedFeatureCollectionSource(Operation getFeatureOperation, SimpleFeatureCollection featureCollection, Catalog catalog, ServletContext context, JDBCDataStore dataStore) {
        this.featureIterator = featureCollection.features();
        this.catalog = catalog;
        this.context = context;

        // Retrieve config params stored in pivot.xml

        PivotConfigFile pivotConfigurationFile = new PivotConfigFile(getDataDirectory(getFeatureOperation));
        this.pivotConfig = pivotConfigurationFile.getConfig();

        //  Get feature type definition
        
        SimpleFeatureType ft = featureCollection.getSchema();

        this.nonPivotColumnNames = new ArrayList<>();
        pivotSourceColumnNames = new ArrayList<>();

        // Determine pivoted (JSON) and non-pivoted columns

        for (AttributeDescriptor descriptor: ft.getAttributeDescriptors()) {
            String attributeName = descriptor.getLocalName();
            String attributeType = descriptor.getUserData().size() > 0 ? descriptor.getUserData().get("org.geotools.jdbc.nativeTypeName").toString() : "";

            if (attributeType.equals(JSON_FIELD_TYPE)) {
                pivotSourceColumnNames.add(attributeName);
            } else if (!pivotConfig.getExcludedFields().contains(attributeName)) {
                this.nonPivotColumnNames.add(attributeName);
            }
        }

        // Loop through all attributes of the JSONB columns to build the pivot fields columns names

        Connection cx = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Set<String> foundColumnNames = new HashSet<>();

        String typeName = featureCollection.getSchema().getName().toString();
        FeatureTypeInfo fi = catalog.getFeatureTypeByName(typeName);
        String sourceTable = String.format("%s.%s", dataStore.getDatabaseSchema(), fi.getNativeName());

        try {

            for (String sourceColumnName : pivotSourceColumnNames) {
                cx = dataStore.getConnection(Transaction.AUTO_COMMIT);
                stmt = cx.prepareStatement(
                        String.format("SELECT DISTINCT jsonb_object_keys(%s) FROM %s", sourceColumnName, sourceTable));
                rs = stmt.executeQuery();

                while (rs.next()) {
                    String value = rs.getString(1);
                    if (value != null) {
                        foundColumnNames.add(value);
                    }
                }
            }
        }
        catch (SQLException | IOException e) {
            LOGGER.warning(e.getMessage());
        }
        finally {
            dataStore.closeSafe(rs);
            dataStore.closeSafe(stmt);
            dataStore.closeSafe(cx);
        }

        this.pivotColumnNames = foundColumnNames.stream().sorted().collect(Collectors.toList());

        // Apply new sort direction if different from default

        if (pivotConfig.getOrderDirection() != null && pivotConfig.getOrderDirection().equalsIgnoreCase("desc")) {
            this.pivotColumnNames.sort(Collections.reverseOrder());
        }

        // Initialise the first row of data

        if (featureIterator.hasNext()) {
            nextFeature = featureIterator.next();
        }

    }

    @Override
    public List<String> getColumnNames() {
        List<String> result = new ArrayList<>();
        result.addAll(nonPivotColumnNames);
        result.addAll(pivotColumnNames);
        return result;
    }

    @Override
    public boolean hasNext() {
        // is there another feature
        return nextFeature != null;
    }

    @Override
    public List<Object> next() {
        // check to see if there are any more features
        if (nextFeature == null) {
            throw new UnsupportedOperationException();
        }

        List<Object> result = new ArrayList<>();

        // Add non-pivot attribute values to column values
        for (String columnName: nonPivotColumnNames) {
            result.add(nextFeature.getAttribute(columnName));
        }

        // Add pivoted attribute values to column values. If missing in row use default value

        JSONParser parser = new JSONParser();
        for (String sourceColumnName : pivotSourceColumnNames) {
            String column_json_string = (String) nextFeature.getAttribute(sourceColumnName);

            if(StringUtils.isNotBlank(column_json_string)) {
                try {
                    JSONObject json = (JSONObject) parser.parse(column_json_string);
                    for (String key : this.pivotColumnNames.stream().sorted().collect(Collectors.toList())) {
                        if(json.containsKey(key)) {
                            result.add(json.get(key));
                        } else {
                            result.add(this.pivotConfig.getDefaultValue());
                        }
                    }
                } catch (ParseException pe) {
                    LOGGER.warning(pe.getMessage());
                }
            } else {
                for (String key : this.pivotColumnNames.stream().sorted().collect(Collectors.toList())) {
                    result.add("");
                }
            }
        }

        if (featureIterator.hasNext()) {
            nextFeature = featureIterator.next();
        } else {
            nextFeature = null;
        }

        return result;
    }

    @Override
    public void close() {
        featureIterator.close();
    }

    private String getDataDirectory(Operation operation) {
        return new DataDirectory(context).getLayerDataDirectoryPath(catalog, operation);
    }
}
