package au.org.emii.wps.gogoduck.index;

import au.org.emii.download.DownloadRequest;
import au.org.emii.wps.gogoduck.exception.GoGoDuckException;
import au.org.emii.wps.gogoduck.parameter.SubsetParameters;
import org.geoserver.catalog.Catalog;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.time.CalendarDateRange;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class FeatureSourceIndexReader implements IndexReader {
    private static final Logger logger = LoggerFactory.getLogger(FeatureSourceIndexReader.class);
    private final URLMangler urlMangler;

    private Catalog catalog = null;

    public FeatureSourceIndexReader(Catalog catalog, Map<String, String> urlSubstitution) {
        this.catalog = catalog;
        this.urlMangler = new URLMangler(urlSubstitution);
    }

    @Override
    public Set<DownloadRequest> getDownloadList(String typeName, String timeField, String sizeField, String urlField, SubsetParameters subset) throws GoGoDuckException {
        Set<DownloadRequest> result = new LinkedHashSet<>();

        Filter cqlFilter = getFilter(timeField, subset.getTimeRange());

        Query query = new Query(typeName, cqlFilter, new String[]{urlField, sizeField});

        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
        SortBy sortByTimeAscending = ff.sort(timeField, SortOrder.ASCENDING);

        query.setSortBy(new SortBy[]{sortByTimeAscending});

        try (SimpleFeatureIterator iterator = getFeatures(typeName, query)) {
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                String url = (String) feature.getAttribute(urlField);
                long size = Math.round((Double)feature.getAttribute(sizeField));
                logger.info(String.format("Processing url '%s'", url));
                result.add(new DownloadRequest(urlMangler.mangle(new URI(url)), size));
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new GoGoDuckException(String.format("Could not obtain list of downloads: '%s'", e.getMessage()));
        }

        return result;
    }

    private Filter getFilter(String timeField, CalendarDateRange timeRange) {
        if (timeRange == null) {
            return Filter.INCLUDE;
        }

        String timeCoverageStart = timeRange.getStart().toString();
        String timeCoverageEnd = timeRange.getEnd().toString();

        try {
            return CQL.toFilter(
                    String.format("%s >= '%s' AND %s <= '%s'",
                            timeField, timeCoverageStart,
                            timeField, timeCoverageEnd)
            );
        } catch (CQLException e) {
            logger.error(e.getMessage(), e);
            throw new GoGoDuckException(e.getMessage());
        }
    }

    private SimpleFeatureIterator getFeatures(String typeName, Query query) throws IOException {
        SimpleFeatureSource featureSource = (SimpleFeatureSource) catalog.getFeatureTypeByName(typeName).getFeatureSource(null, null);
        SimpleFeatureCollection collection = featureSource.getFeatures(query);
        return collection.features();
    }
}
