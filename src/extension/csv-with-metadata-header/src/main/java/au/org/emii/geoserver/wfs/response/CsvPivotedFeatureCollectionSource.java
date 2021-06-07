package au.org.emii.geoserver.wfs.response;

import org.geoserver.catalog.Catalog;
import org.geoserver.platform.Operation;
import org.geoserver.wfs.TypeInfoCollectionWrapper;
import org.geotools.data.FeatureSource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.visitor.UniqueVisitor;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.postgresql.util.PGobject;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static au.org.emii.geoserver.wfs.response.CSVWithMetadataHeaderOutputFormat.JSON_FIELD_TYPE;
import static org.geotools.gce.imagemosaic.Utils.FF;


public class CsvPivotedFeatureCollectionSource implements CsvSource {

    private static final int MAX_PIVOT_COLUMNS = 10000;
    private final SimpleFeatureIterator featureIterator;
    private final ArrayList<String> nonPivotColumnNames;
    private final List<String> pivotColumnNames;
    private final List<String> pivotSourceColumnNames;
    private final PivotConfig pivotConfig;

    static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("au.org.emii.geoserver.wfs.response");

    private SimpleFeature nextFeature;
    private final Catalog catalog;
    private final ServletContext context;

    public CsvPivotedFeatureCollectionSource(Operation getFeatureOperation, SimpleFeatureCollection featureCollection, Catalog catalog, ServletContext context) {
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

        // Read the first row to get JSON attr for column names

        if (featureIterator.hasNext()) {
            nextFeature = featureIterator.next();
        }

        // Loop through each source column of JSON objects and build up a list of distinct attribute names
        // alphabetically to then be rendered into columns

        JSONParser parser = new JSONParser();
        Set<String> foundColumnNames = new HashSet<>();
        for (String sourceColumnName : pivotSourceColumnNames) {
            try {
                FeatureSource featureSource = ((TypeInfoCollectionWrapper.Simple) featureCollection)
                        .getFeatureTypeInfo().getFeatureSource(null, null);
                SimpleFeatureCollection features = (SimpleFeatureCollection) featureSource.getFeatures();

                UniqueVisitor visitor = new UniqueVisitor(FF.property(sourceColumnName)) {
                    @Override
                    public boolean hasLimits() {
                        // force usage of visitor limits, also for size extraction "query"
                        return true;
                    }
                };

                visitor.setMaxFeatures(MAX_PIVOT_COLUMNS);
                features.accepts(visitor, null);
                if (visitor.getResult() != null && visitor.getResult().toList() != null) {
                    for (Object result : visitor.getResult().toList()) {
                        try {
                            JSONObject json = (JSONObject) parser.parse(((PGobject) result).getValue());
                            foundColumnNames.addAll(json.keySet());
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        this.pivotColumnNames = foundColumnNames.stream().sorted().collect(Collectors.toList());

        // Apply new sort direction if different from default

        if (pivotConfig.getOrderDirection() != null && pivotConfig.getOrderDirection().equalsIgnoreCase("desc")) {
            this.pivotColumnNames.sort(Collections.reverseOrder());
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
            try {
                JSONObject json = (JSONObject) parser.parse(column_json_string);
                for (String key : this.pivotColumnNames.stream().sorted().collect(Collectors.toList())){
                    result.add(json.getOrDefault(key, this.pivotConfig.getDefaultValue()));
                }
            } catch(ParseException pe) {
                LOGGER.warning(pe.getMessage());
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
