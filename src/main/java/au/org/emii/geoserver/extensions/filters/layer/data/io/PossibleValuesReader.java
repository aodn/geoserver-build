package au.org.emii.geoserver.extensions.filters.layer.data.io;

import au.org.emii.geoserver.extensions.filters.layer.data.Filter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Feature;

import java.io.IOException;
import java.util.*;

public class PossibleValuesReader {

    public List<Filter> read(LayerInfo layerInfo, List<Filter> filters) throws IOException {
        setFilterValues(getValueFilters(filters), getFeatureSource(layerInfo).getFeatures());

        return filters;
    }

    private void setFilterValues(List<Filter> filters, FeatureCollection featureCollection) throws IOException {
        FeatureIterator features = featureCollection.features();
        try {
            Map<String, Set> filterValues = initFilterValues(filters);

            while (features.hasNext()) {
                collectFilterValues(features.next(), filters, filterValues);
            }
        }
        finally {
            features.close();
        }
    }

    private Map<String, Set> initFilterValues(List<Filter> filters) {
        Map<String, Set> filterValues = new LinkedHashMap<String, Set>(filters.size());
        for (Filter filter : filters) {
            filter.setValues(new TreeSet<String>());
            filterValues.put(filter.getName(), filter.getValues());
        }

        return filterValues;
    }

    private void collectFilterValues(Feature feature, List<Filter> filters, Map<String, Set> filterValues) {
        for (Filter filter : filters) {
            filterValues.get(filter.getName()).add(getFeaturePropertyValue(feature, filter.getName()));
        }
    }

    private Object getFeaturePropertyValue(Feature feature, String propertyName) {
        return feature.getProperty(propertyName).getValue();
    }

    private FeatureSource getFeatureSource(LayerInfo layerInfo) throws IOException {
        return ((FeatureTypeInfo)layerInfo.getResource()).getFeatureSource(null, null);
    }

    private List<Filter> getValueFilters(List<Filter> filters) {
        List<Filter> possibleValueFilters = new ArrayList<Filter>(filters);
        CollectionUtils.filter(possibleValueFilters, getPredicate());
        return possibleValueFilters;
    }

    private Predicate getPredicate() {
        return new Predicate() {
            public boolean evaluate(Object o) {
                return "string".equals(((Filter)o).getType().toLowerCase());
            }
        };
    }
}
