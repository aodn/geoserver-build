package au.org.emii.gogoduck.worker;

import au.org.emii.gogoduck.exception.GoGoDuckException;
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

import java.io.IOException;
import java.net.URI;

public class FeatureSourceIndexReader implements IndexReader {
    private static final Logger logger = LoggerFactory.getLogger(FeatureSourceIndexReader.class);

    private Catalog catalog = null;

    public FeatureSourceIndexReader(Catalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public URIList getUriList(String profile, String timeField, String sizeField, String urlField, GoGoDuckSubsetParameters subset) throws GoGoDuckException {

        URIList uriList = new URIList();

        String timeCoverageStart = subset.get("TIME").start;
        String timeCoverageEnd = subset.get("TIME").end;

        // TODO Should include also workspace, but works also without
        String typeName = profile;

        Filter cqlFilter = null;

        try {
            cqlFilter = CQL.toFilter(
                    String.format("%s >= '%s' AND %s <= '%s'",
                            timeField, timeCoverageStart,
                            timeField, timeCoverageEnd)
            );
        } catch (CQLException e) {
            throw new GoGoDuckException(e.getMessage());
        }

        Query query = new Query(typeName, cqlFilter, new String[]{urlField, sizeField});

        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
        SortBy sortByTimeAscending = ff.sort(timeField, SortOrder.ASCENDING);

        query.setSortBy(new SortBy[]{sortByTimeAscending});
        double totalFileSize = 0.0;

        try (SimpleFeatureIterator iterator = getFeatures(typeName, query)) {
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                String url = (String) feature.getAttribute(urlField);
                logger.info(String.format("Processing url '%s'", url));
                uriList.add(new URI(url));

                try {
                    totalFileSize+= (double) feature.getAttribute(sizeField);
                    logger.info(String.format("Processing size '%s'", totalFileSize));
                } catch (NullPointerException e) {
                    logger.warn(String.format("Unable to get size field for layer %s", profile));
                }
            }
        } catch (Exception e) {
            throw new GoGoDuckException(String.format("Could not obtain list of URLs: '%s'", e.getMessage()));
        }

        uriList.setTotalFileSize(totalFileSize);
        return uriList;
    }

    private SimpleFeatureIterator getFeatures(String typeName, Query query) throws IOException {
        SimpleFeatureSource featureSource = (SimpleFeatureSource) catalog.getFeatureTypeByName(typeName).getFeatureSource(null, null);
        SimpleFeatureCollection collection = featureSource.getFeatures(query);
        return collection.features();
    }
}
