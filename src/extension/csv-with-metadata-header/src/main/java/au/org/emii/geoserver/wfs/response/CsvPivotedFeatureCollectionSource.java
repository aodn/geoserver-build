package au.org.emii.geoserver.wfs.response;

import org.geoserver.platform.Operation;
import org.geoserver.wfs.TypeInfoCollectionWrapper.Simple;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs.request.Query;
import org.geotools.data.FeatureSource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.visitor.UniqueVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.sort.SortBy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static org.geotools.gce.imagemosaic.Utils.FF;

public class CsvPivotedFeatureCollectionSource implements CsvSource {

    private static final int MAX_PIVOT_COLUMNS = 10000;
    private final SimpleFeatureCollection featureCollection;
    private final SimpleFeatureIterator featureIterator;
    private final String pivotName;
    private final String pivotValue;
    private final List<String> sortAttributes;
    private final ArrayList<String> nonPivotColumnNames;
    private final List<String> pivotColumnNames;

    private SimpleFeature nextFeature;

    public CsvPivotedFeatureCollectionSource(Operation getFeatureOperation, SimpleFeatureCollection featureCollection) {
        this.featureCollection = featureCollection;
        this.featureIterator = featureCollection.features();

        //  Get feature type definition
        
        SimpleFeatureType ft = featureCollection.getSchema();

        // Get pivot parameters from request

        GetFeatureRequest request = GetFeatureRequest.adapt(getFeatureOperation.getParameters()[0]);

        Map<String, ?> formatOptions = request.getFormatOptions();
        this.pivotName = (String)formatOptions.get("PIVOT_NAME");
        this.pivotValue = (String)formatOptions.get("PIVOT_VALUE");

        if (this.pivotName == null) {
            throw new IllegalArgumentException("format options must contain pivot_name");
        }

        if (ft.getDescriptor(this.pivotName) == null) {
            throw new IllegalArgumentException("pivot_name must be an attribute of this feature");
        }

        if (this.pivotValue == null) {
            throw new IllegalArgumentException("format options must contain pivot_value");
        }

        if (ft.getDescriptor(this.pivotValue) == null) {
            throw new IllegalArgumentException("pivot_value must be an attribute of this feature");
        }

        // Determine sort attributes

        List<Query> queries = request.getQueries();
        Query query = queries.get(0);
        List<SortBy> sortBys = query.getSortBy();

        if (sortBys == null || sortBys.size() == 0) {
            throw new IllegalArgumentException("the request must contain sort attributes for pivoting");
        }

        this.sortAttributes = new ArrayList<>();

        for (SortBy sortBy: sortBys) {
            sortAttributes.add(sortBy.getPropertyName().getPropertyName());
        }

        // Determine non-pivoted columns

        this.nonPivotColumnNames = new ArrayList<>();

        for (AttributeDescriptor descriptor: ft.getAttributeDescriptors()) {
            String attributeName = descriptor.getLocalName();

            if (!attributeName.equals(pivotName) && !attributeName.equals(pivotValue)) {
                this.nonPivotColumnNames.add(attributeName);
            }
        }

        // Determine pivoted column names by querying feature source for unique pivot_name values

        this.pivotColumnNames = new ArrayList<>();

        try {
            FeatureSource featureSource = ((Simple) featureCollection).getFeatureTypeInfo().getFeatureSource(null, null);
            SimpleFeatureCollection features = (SimpleFeatureCollection) featureSource.getFeatures();

            UniqueVisitor visitor = new UniqueVisitor(FF.property(pivotName)) {
                @Override
                public boolean hasLimits() {
                    // force usage of visitor limits, also for size extraction "query"
                    return true;
                }
            };

            visitor.setMaxFeatures(MAX_PIVOT_COLUMNS);
            visitor.setPreserveOrder(true);
            features.accepts(visitor, null);

            if (visitor.getResult() != null && visitor.getResult().toList() != null) {
                this.pivotColumnNames.addAll(visitor.getResult().toList());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Collections.sort(this.pivotColumnNames, Comparator.nullsLast(Comparator.naturalOrder()));

        // Read the next feature to start pivoting from if there is one

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
        // is there another feature to start pivoting from
        return nextFeature != null;
    }

    @Override
    public List<Object> next() {
        // check to see if there are any more features to pivot
        if (nextFeature == null) {
            throw new UnsupportedOperationException();
        }

        List<Object> result = new ArrayList<>();
        Map<String, Object> currentGroup = new HashMap();

        // Store current group details
        
        for (String sortAttribute: sortAttributes) {
            currentGroup.put(sortAttribute, nextFeature.getAttribute(sortAttribute));
        }

        // Add non-pivot attribute values to column values

        for (String columnName: nonPivotColumnNames) {
            result.add(nextFeature.getAttribute(columnName));
        }

        //  Pivot column values in current group until there aren't any more

        Map<String, Object> pivotedColumnValues = new HashMap<>();

        while (nextFeature != null && inGroup(nextFeature, currentGroup)) {
            pivotedColumnValues.put((String)nextFeature.getAttribute(pivotName), nextFeature.getAttribute(pivotValue));

            if (featureIterator.hasNext()) {
                nextFeature = featureIterator.next();
            } else {
                nextFeature = null;
            }
        }

        // Add pivoted column values to column values

        for (String columnName: pivotColumnNames) {
            result.add(pivotedColumnValues.get(columnName));
        }

        return result;
    }

    private boolean inGroup(SimpleFeature nextFeature, Map<String, Object> currentGroup) {
        for (Entry<String, Object> groupAttribute: currentGroup.entrySet()) {
            String groupAttributeName = groupAttribute.getKey();
            Object groupAttributeValue = groupAttribute.getValue();
            Object featureAttributeValue = nextFeature.getAttribute(groupAttributeName);

            if (!featureAttributeValue.equals(groupAttributeValue)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void close() {
        featureIterator.close();
    }
}
