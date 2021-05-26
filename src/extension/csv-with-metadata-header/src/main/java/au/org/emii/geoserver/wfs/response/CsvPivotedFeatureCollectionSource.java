package au.org.emii.geoserver.wfs.response;

import org.geoserver.platform.Operation;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static au.org.emii.geoserver.wfs.response.CSVWithMetadataHeaderOutputFormat.JSON_FIELD_TYPE;


public class CsvPivotedFeatureCollectionSource implements CsvSource {

    private final SimpleFeatureCollection featureCollection;
    private final SimpleFeatureIterator featureIterator;
    private final ArrayList<String> nonPivotColumnNames;
    private final List<String> pivotColumnNames;
    private final List<String> pivotSourceColumnNames;

    static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("au.org.emii.geoserver.wfs.response");

    private SimpleFeature nextFeature;

    public CsvPivotedFeatureCollectionSource(Operation getFeatureOperation, SimpleFeatureCollection featureCollection) {
        this.featureCollection = featureCollection;
        this.featureIterator = featureCollection.features();

        //  Get feature type definition
        
        SimpleFeatureType ft = featureCollection.getSchema();

        // Get pivot parameters from request

        GetFeatureRequest request = GetFeatureRequest.adapt(getFeatureOperation.getParameters()[0]);

        this.nonPivotColumnNames = new ArrayList<>();
        pivotSourceColumnNames = new ArrayList<>();

        // Determine pivoted (JSON) and non-pivoted columns

        for (AttributeDescriptor descriptor: ft.getAttributeDescriptors()) {
            String attributeName = descriptor.getLocalName();
            String attributeType = descriptor.getUserData().get("org.geotools.jdbc.nativeTypeName").toString();

            if (attributeType.equals(JSON_FIELD_TYPE)) {
                pivotSourceColumnNames.add(attributeName);
            } else {
                this.nonPivotColumnNames.add(attributeName);
            }
        }

        // Read the first row to get JSON attr for column names

        if (featureIterator.hasNext()) {
            nextFeature = featureIterator.next();
        }

        // Load JSON field value and list attribute/column names, sorted alphabetically

        this.pivotColumnNames = new ArrayList<>();
        for (String sourceColumnName : pivotSourceColumnNames) {
            String column_json_string = (String) nextFeature.getAttribute(sourceColumnName);
            JSONParser parser = new JSONParser();
            try {
                JSONObject json = (JSONObject) parser.parse(column_json_string);
                Set<String> keys = json.keySet();
                this.pivotColumnNames.addAll(keys.stream().sorted().collect(Collectors.toList()));
            } catch(ParseException pe) {
                LOGGER.warning(pe.getMessage());
            }
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
        Map<String, Object> currentGroup = new HashMap();

        // Add non-pivot attribute values to column values
        for (String columnName: nonPivotColumnNames) {
            result.add(nextFeature.getAttribute(columnName));
        }

        // Add pivoted column values to column values
        for (String sourceColumnName : pivotSourceColumnNames) {
            String column_json_string = (String) nextFeature.getAttribute(sourceColumnName);
            JSONParser parser = new JSONParser();
            try {
                JSONObject json = (JSONObject) parser.parse(column_json_string);
                Set<String> keys = json.keySet();
                for (String key : keys.stream().sorted().collect(Collectors.toList())){
                    result.add(json.get(key));
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
}
